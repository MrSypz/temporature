package com.sypztep.common;

import com.sypztep.Temporature;
import com.sypztep.TemporatureServerConfig;
import com.sypztep.plateau.common.api.PlateauDamageTypes;
import com.sypztep.system.temperature.TemperatureHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * <ul>
 *   <li>{@code worldTemp} — ambient in MC units (1 MC = 25°C), unclamped</li>
 *   <li>{@code coreTemp}  — body heat accumulator in ±150 scale. Grows when worldTemp is outside
 *       the safe band, drifts to 0 when inside. Damage triggers at ±100.</li>
 *   <li>{@code baseOffset} — static offset applied to core (food, potions)</li>
 * </ul>
 */
public final class PlayerTemperatureComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    private static final int TICK_INTERVAL = 1;
    private static final float BASE_OFFSET_DECAY_PER_SECOND = 0.95f;
    private static final int DANGER_GRACE_TICKS = 100;

    private final Player player;

    private float coreTemp;       // body heat accumulator, ±150 scale
    private float worldTemp;      // ambient in MC units
    private float baseOffset;     // static offset on core (food buff)
    private int baseOffsetTicks;
    private float wetness;

    private int tickCounter;
    private TemperatureHelper.TempZone currentZone = TemperatureHelper.TempZone.NORMAL;

    private int graceTicks;
    private int damageCooldown;
    private float lastCoreTemp;

    private static final String
            CORE_TEMP_TAG = "CoreTemp",
            WORLD_TEMP_TAG = "WorldTemp",
            BASE_OFFSET_TAG = "BaseOffset",
            BASE_OFFSET_TICKS_TAG = "BaseOffsetTicks",
            WETNESS_TAG = "Wetness",
            GRACE_TICKS_TAG = "GraceTicks";

    public PlayerTemperatureComponent(Player player) {
        this.player = player;
    }

    /**
     * Current core accumulator (±150 scale).
     */
    public float getBodyTemp() { return coreTemp; }

    /**
     * Current ambient temperature in MC units.
     */
    public float getWorldTemp() { return worldTemp; }
    /**
     * Static core offset from food/potions (±150 scale).
     */
    public float getBaseOffset() { return baseOffset; }
    public float getWetness() { return wetness;
    }

    public TemperatureHelper.TempZone getZone() { return currentZone; }

    /**
     * Add a decaying core-temp offset on the ±150 scale (e.g. +10 for warm food).
     */
    public void addBaseOffset(float coreDelta, int durationTicks) {
        this.baseOffset += coreDelta;
        this.baseOffsetTicks = Math.max(this.baseOffsetTicks, durationTicks);
        TemperatureEntityComponents.PLAYER_TEMPERATURE.sync(player);
    }

    @Override
    public void serverTick() {
        TemporatureServerConfig config = TemporatureServerConfig.getInstance();
        if (!config.enableTemperatureSystem) return;

        tickWetness();

        // Compute world temperature (MC units, unclamped)
        double world = TemperatureHelper.calculateWorldTemp(player);
        this.worldTemp = (float) world;

        // Accumulate core temperature — two-step model matching Cold Sweat exactly.
        int sign = TemperatureHelper.worldTempSign(world);
        double core = coreTemp;

        boolean immuneToTemp = player.isCreative() || player.isSpectator()
                || player.level().getDifficulty() == Difficulty.PEACEFUL;

        // Accumulation toward hot/cold (runs only when outside safe band)
        if (sign != 0 && !immuneToTemp) {
            double difference = TemperatureHelper.safeBandDifference(world);
            double changeBy = Math.max(
                    (difference / 7.0) * config.tempRate,
                    Math.abs(config.tempRate / 50.0)
            ) * sign;

//            // Nutrition influences the rate
//            PlayerNutritionComponent nutrition = TemperatureEntityComponents.PLAYER_NUTRITION.get(player);
//            double hydPct = nutrition.getHydration() / Math.max(1, nutrition.getMaxHydration());
//            double energyPct = nutrition.getEnergy() / Math.max(1, nutrition.getMaxEnergy());
//            if (changeBy > 0) {
//                if (hydPct > 0.70) changeBy *= 0.80;
//                else if (hydPct < 0.30) changeBy *= 1.30;
//            } else {
//                if (energyPct > 0.50) changeBy *= 0.80;
//                else if (energyPct < 0.10) changeBy *= 1.40;
//            }

            core += changeBy;
        }

        // Drift toward 0 — runs every tick when the core has a sign that
        // differs from the world's sign (e.g. hot body in a safe world, or cold body
        // in a hot world). Matches Cold Sweat's blend-back behavior.
        int coreSign = (int) Math.signum(core);
        if (coreSign != 0 && coreSign != sign && !immuneToTemp) {
            double edge = (coreSign == 1)
                    ? TemperatureHelper.maxHabitable()
                    : TemperatureHelper.minHabitable();
            double amount = (world - edge) / 3.0;
            double changeBy = maxAbs(amount * config.tempRate, config.tempRate / 10.0 * -coreSign);
            // Don't cross zero
            changeBy = minAbs(changeBy, -core);
            core += changeBy;
        }

        // Decay the base offset
        if (baseOffsetTicks > 0) {
            baseOffsetTicks--;
            if (baseOffsetTicks <= 0) {
                baseOffset = 0;
            } else {
                float perTickDecay = (float) Math.pow(BASE_OFFSET_DECAY_PER_SECOND, 1.0 / 20.0);
                baseOffset *= perTickDecay;
            }
        }

        // Clamp core to ±150
        coreTemp = (float) TemperatureHelper.clampCore(core);

        //Determine zone on (core + base)
        float effective = coreTemp + baseOffset;
        TemperatureHelper.TempZone zone = TemperatureHelper.zoneFor(effective);
        int prevTier = dangerTier(currentZone);
        int curTier = dangerTier(zone);
        if (zone != currentZone) {
            if (curTier > prevTier) graceTicks = DANGER_GRACE_TICKS;
            currentZone = zone;
        }

        // Apply damage if in danger zone
        tickInlinedDamage(curTier, effective);
        lastCoreTemp = coreTemp;

        tickCounter++;
        if (tickCounter >= TICK_INTERVAL) {
            tickCounter = 0;
            TemperatureEntityComponents.PLAYER_TEMPERATURE.sync(player);
        }
    }
    @Override
    public void clientTick() {
        if (wetness > 0 && !player.isInWater()) {
            if (player.getRandom().nextFloat() < wetness) {
                double rx = player.getBbWidth() * (player.getRandom().nextDouble() - 0.5);
                double ry = player.getBbHeight() * player.getRandom().nextDouble();
                double rz = player.getBbWidth() * (player.getRandom().nextDouble() - 0.5);

                player.level().addParticle(ParticleTypes.FALLING_WATER,
                        player.getX() + rx, player.getY() + ry, player.getZ() + rz,
                        0,0,0);
            }
        }
    }
    /**
     * Damage tick. Tier 0 = no damage, Tier 1 = HYPOTHERMIA/HEATSTROKE, Tier 2 = FREEZING/BURNING.
     */
    private void tickInlinedDamage(int tier, float effectiveCore) {
        if (tier <= 0) {
            graceTicks = 0;
            damageCooldown = 0;
            return;
        }
        if (player.isCreative() || player.isSpectator()) return;           // skip creative/spectator check early to avoid unnecessary calculations
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) return; // skip peaceful same reason as creative/spectator
        if (!(player.level() instanceof ServerLevel)) return;

        // Actually only tier 2 (BURNING/FREEZING) deals damage.
        // Tier 1 (HEATSTROKE/HYPOTHERMIA) is a warning zone with no damage yet.
        if (tier < 2) {
            graceTicks = 0;
            damageCooldown = 0;
            return;
        }

        if (graceTicks > 0) {
            graceTicks--;
            return;
        }

        TemporatureServerConfig config = TemporatureServerConfig.getInstance();
        boolean hot = effectiveCore > 0;

        // Rate-aware interval: shrinks if worsening, stretches if recovering
        float dT = coreTemp - lastCoreTemp;
        int zoneDir = hot ? 1 : -1;
        float intervalMul;
        if (dT * zoneDir > 0) intervalMul = 0.7f;
        else if (dT * zoneDir < 0) intervalMul = 1.5f;
        else intervalMul = 1.0f;
        int effectiveInterval = Math.max(1, Math.round(config.tempDamageInterval * intervalMul));

        if (damageCooldown > 0) {
            damageCooldown--;
            return;
        }
        damageCooldown = effectiveInterval;

        float damage = config.tempBaseDamage; // as max health percentage
        if (damage <= 0) return;

        PlateauDamageTypes.hurt(player, hot
                        ? player.damageSources().source(Temporature.HEATSTROKE)
                        : player.damageSources().source(Temporature.HYPOTHERMIA),
                damage);
    }

    private static int dangerTier(TemperatureHelper.TempZone zone) {
        return switch (zone) {
            case HYPOTHERMIA, HEATSTROKE -> 1;
            case FREEZING, BURNING -> 2;
            default -> 0;
        };
    }
    // ternary style
    /**
     * Returns whichever value has the larger absolute magnitude, preserving sign.
     */
    private static double maxAbs(double a, double b) { return Math.abs(a) >= Math.abs(b) ? a : b; }

    /**
     * Returns whichever value has the smaller absolute magnitude, preserving sign.
     */
    private static double minAbs(double a, double b) { return Math.abs(a) <= Math.abs(b) ? a : b; }

    private void tickWetness() {
        boolean inWater = player.isInWater() || player.isUnderWater();

        if (inWater) wetness = Math.min(1f, wetness + 0.02f);
        else if (player.level().isRainingAt(player.blockPosition())) wetness = Math.min(1f, wetness + 0.005f);
         else {
            // Dry faster when the core is hot, slower when cold
            float core = coreTemp + baseOffset;
            float dryRate = 0.0008f;
            if (core > TemperatureHelper.WARM_DEV) dryRate += (core / 100f) * 0.0008f;
            else if (core < TemperatureHelper.HYPOTHERMIA_DEV) dryRate *= 0.3f;
            wetness = Math.max(0f, wetness - dryRate);
        }
    }

    public double getHydrationDrainMultiplier() {
        float core = coreTemp + baseOffset;
        if (core > TemperatureHelper.WARM_DEV) {
            // Scale up with how hot you feel, capped by config multiplier
            return 1.0 + (core - TemperatureHelper.WARM_DEV) / 75.0
                    * TemporatureServerConfig.getInstance().hotHydrationDrainMul;
        }
        if (core < TemperatureHelper.CHILLY_DEV) return 0.9;
        return 1.0;
    }

    public double getEnergyDrainMultiplier() {
        float core = coreTemp + baseOffset;
        if (core < TemperatureHelper.CHILLY_DEV) {
            return 1.0 + Math.abs(core - TemperatureHelper.CHILLY_DEV) / 75.0
                    * TemporatureServerConfig.getInstance().coldEnergyDrainMul;
        }
        return 1.0;
    }

    @Override
    public void readData(ValueInput input) {
        coreTemp = input.getFloatOr(CORE_TEMP_TAG, 0f);
        worldTemp = input.getFloatOr(WORLD_TEMP_TAG, 0f);
        baseOffset = input.getFloatOr(BASE_OFFSET_TAG, 0f);
        baseOffsetTicks = input.getIntOr(BASE_OFFSET_TICKS_TAG, 0);
        wetness = input.getFloatOr(WETNESS_TAG, 0f);
        graceTicks = input.getIntOr(GRACE_TICKS_TAG, 0);
        lastCoreTemp = coreTemp;
    }

    @Override
    public void writeData(ValueOutput output) {
        output.putFloat(CORE_TEMP_TAG, coreTemp);
        output.putFloat(WORLD_TEMP_TAG, worldTemp);
        output.putFloat(BASE_OFFSET_TAG, baseOffset);
        output.putInt(BASE_OFFSET_TICKS_TAG, baseOffsetTicks);
        output.putFloat(WETNESS_TAG, wetness);
        output.putInt(GRACE_TICKS_TAG, graceTicks);
    }
}
