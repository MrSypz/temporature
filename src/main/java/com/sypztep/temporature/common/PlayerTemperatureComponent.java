package com.sypztep.temporature.common;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.temporature.api.TemperatureEvents;
import com.sypztep.temporature.api.TemporatureApi;
import com.sypztep.plateau.common.api.PlateauDamageTypes;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import com.sypztep.temporature.system.temperature.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    // Adaptation constants
    public static final float NEUTRAL_TEMP = 0.5f;
    public static final float ADAPT_RATE = 0.00017f;
    public static final float ADAPT_RATE_SUSTAINED = 0.00034f;
    public static final int SUSTAINED_THRESHOLD = 24000;
    public static final float BIOME_JUMP_THRESHOLD = 0.5f;

    private final Player player;

    private float coreTemp;       // body heat accumulator, ±150 scale
    private float worldTemp;      // ambient in MC units
    private float baseOffset;     // static offset on core (food buff)
    private int baseOffsetTicks;
    private float wetness;
    private float waterTempAccum;

    // Adaptation state
    private float adaptedBiomeTemp = NEUTRAL_TEMP;
    private int adaptExposureTicks;
    private float lastBiomeBase = NEUTRAL_TEMP; // transient, not persisted

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
            WATER_TEMP_ACCUM_TAG = "WaterTempAccum",
            GRACE_TICKS_TAG = "GraceTicks",
            ADAPTED_BIOME_TEMP_TAG = "AdaptedBiomeTemp",
            ADAPT_EXPOSURE_TICKS_TAG = "AdaptExposureTicks";

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
    public float getWetness() { return wetness; }

    /**
     * Accumulated water temperature in MC units. Ramps toward water temp while submerged,
     * decays toward 0 while drying. Used by WetnessLayer for worldTemp offset.
     */
    public float getWaterTempAccum() { return waterTempAccum; }

    public TemperatureHelper.TempZone getZone() { return currentZone; }

    public float getAdaptedBiomeTemp() { return adaptedBiomeTemp; }
    public int getAdaptExposureTicks() { return adaptExposureTicks; }
    public float getAdaptationStrength() {
        TemporatureServerConfig config = TemporatureServerConfig.getInstance();
        float shift = Math.abs(adaptedBiomeTemp - NEUTRAL_TEMP);
        return Math.min(shift / config.maxAdaptShift, 1.0f);
    }

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
        boolean immuneToTemp = player.isCreative() || player.isSpectator()
                || player.level().getDifficulty() == Difficulty.PEACEFUL;

        if (!player.isSpectator()) tickWetness(); // spectators can't wet

        double world = TemperatureHelper.calculateWorldTemp(player);
        this.worldTemp = (float) world;

        if (config.enableAdaptation) tickAdaptation();

        int sign = TemperatureHelper.worldTempSign(world);
        double core = coreTemp;

        // Accumulation toward hot/cold (runs only when outside safe band)
        if (sign != 0 && !immuneToTemp) {
            double difference = TemperatureHelper.safeBandDifference(world);
            double changeBy = Math.max(
                    (difference / 7.0) * config.tempRate,
                    Math.abs(config.tempRate / 50.0)
            ) * sign;

            changeBy = TemporatureApi.applyRateModifiers(player, changeBy, world, core);

            core += changeBy;
        }

        // Drift toward 0 — runs every tick when the core has a sign that
        // differs from the world's sign (e.g. hot body in a safe world, or cold body
        // in a hot world).
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
            TemperatureHelper.TempZone oldZone = currentZone;
            if (curTier > prevTier) graceTicks = DANGER_GRACE_TICKS;
            currentZone = zone;
            TemperatureEvents.ZONE_CHANGED.invoker().onZoneChanged(player, oldZone, zone);
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
        if (wetness <= 0 || player.isInWater() || player.isSpectator()) return;
        if (!(player == Minecraft.getInstance().player) || Minecraft.getInstance().gameRenderer.getMainCamera().isDetached())
            if (player.getRandom().nextFloat() < wetness) {
                double rx = player.getBbWidth() * (player.getRandom().nextDouble() - 0.5);
                double ry = player.getBbHeight() * player.getRandom().nextDouble();
                double rz = player.getBbWidth() * (player.getRandom().nextDouble() - 0.5);

                player.level().addParticle(ParticleTypes.FALLING_WATER,
                        player.getX() + rx, player.getY() + ry, player.getZ() + rz,
                        0, 0, 0);
            }
    }
    private void tickAdaptation() {
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.level().canSeeSky(player.blockPosition())) return;
        TemporatureServerConfig cfg = TemporatureServerConfig.getInstance();
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        WorldHelper.BiomeTemp bt = WorldHelper.getBiomeTemperature(player.level(), biome);
        float biomeBase = (float) ((bt.lowTemp() + bt.highTemp()) * cfg.threshHoldExtreme);

        if (Math.abs(biomeBase - lastBiomeBase) > BIOME_JUMP_THRESHOLD) {
            adaptExposureTicks = 0;
        }
        lastBiomeBase = biomeBase;
        adaptExposureTicks++;

        float rate = (adaptExposureTicks >= SUSTAINED_THRESHOLD)
                ? ADAPT_RATE_SUSTAINED
                : ADAPT_RATE;
        rate *= cfg.adaptRate / ADAPT_RATE;

        adaptedBiomeTemp += (biomeBase - adaptedBiomeTemp) * rate;
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

        float damage = config.tempBaseDamage;
        if (damage <= 0) return;

        if (!TemperatureEvents.BEFORE_DAMAGE.invoker().beforeDamage(player, currentZone, damage)) return;
        player.hurtServer((ServerLevel) player.level(), hot
                        ? player.damageSources().source(Temporature.HEATSTROKE)
                        : player.damageSources().source(Temporature.HYPOTHERMIA),
                damage);
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
        TemporatureServerConfig config = TemporatureServerConfig.getInstance();
        boolean inWater = player.isInWater() || player.isUnderWater();
        boolean inRain = !inWater && player.level().isRainingAt(player.blockPosition());

        // How much of the player's hitbox is actually in water (0 = tip-toe / none, 1 = head underwater).
        // Scales both wetness growth and the water-temp chase rate so a toe in a puddle barely soaks,
        // while fully submerged behaves exactly as before.
        float submersion = inWater ? computeSubmersion(player) : 0f;

        // --- Wetness scalar (0-1) ---
        // Only the submerged portion of the body can be wet. If wetness exceeds the current
        // submersion level (e.g. fully dunked then stood up waist-deep), the above-water
        // portion dries in air until it equals the submerged fraction.
        float airDry = computeAirDry(config, coreTemp + baseOffset);
        if (inWater) {
            if (wetness < submersion) {
                wetness = Math.min(submersion, wetness + config.waterSoakSpeed * submersion);
            } else if (wetness > submersion) {
                wetness = Math.max(submersion, wetness - airDry);
            }
        } else if (inRain) {
            wetness = Math.min(config.maxRainWetness, wetness + config.rainSoakSpeed);
        } else {
            wetness = Math.max(0f, wetness - airDry);
        }

        // --- Water temp accumulator (MC units) ---
        // Ramps toward biome-specific waterTemp when submerged (depth-adjusted toward deep cold),
        // or rain-specific temp in rain. When neither, drifts toward ambient worldTemp.
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        double biomeWaterTemp = WorldHelper.getWaterTemp(player.level(), biome, config.defaultWaterTemp);

        double target;
        float soakRate;
        if (inWater) {
            int depth = computeWaterDepth(player, config.maxWaterDepth);
            double depthT = Math.min(depth / (double) config.maxWaterDepth, 1.0);
            // Surface = biome water. Deep = universal cold (poor heat conduction + thermocline)
            target = biomeWaterTemp + (config.deepWaterTemp - biomeWaterTemp) * depthT;
            soakRate = config.waterSoakSpeed * submersion;
        } else if (inRain) {
            target = WorldHelper.getRainWaterTemp(player.level(), biome);
            soakRate = config.rainSoakSpeed;
        } else {
            target = 0;
            soakRate = 0;
        }

        if (soakRate > 0) {
            // Ramp toward target
            double diff = target - waterTempAccum;
            double step = Math.min(Math.abs(diff), soakRate) * Math.signum(diff);
            waterTempAccum += (float) step;
        }

        // Dry accumulator proportionally to air-dry rate (applies whenever above-water
        // body surface is exposed — i.e. submersion < 1).
        if (submersion < 1f) {
            waterTempAccum = shrink(waterTempAccum, airDry * 5f);
        }

        // Residual water drifts toward ambient worldTemp (water on skin reaches air temp over time)
        if (!inWater && !inRain && wetness > 0 && waterTempAccum != 0) {
            float driftRate = config.residualWaterDriftRate;
            float core = coreTemp + baseOffset;
            if (core > TemperatureHelper.WARM_DEV) driftRate *= (1f + core / 100f);
            else if (core < TemperatureHelper.HYPOTHERMIA_DEV) driftRate *= config.coldDryMultiplier;
            waterTempAccum += (worldTemp - waterTempAccum) * driftRate;
        }

        // Fire interaction: evaporate water and clear fire
        if (player.level() instanceof ServerLevel && player.isOnFire()) {
            waterTempAccum = shrink(waterTempAccum, 0.1f);
            if (wetness > 0) {
                wetness = Math.max(0f, wetness - 0.05f);
                player.clearFire();
            }
        }

        // Zero out accumulator when fully dry
        if (wetness <= 0) waterTempAccum = 0;
    }

    /**
     * Per-tick air-dry rate, temperature-modulated. Hotter bodies dry faster, cold bodies slower.
     */
    private static float computeAirDry(TemporatureServerConfig config, float core) {
        float dry = config.dryRate;
        if (core > TemperatureHelper.WARM_DEV) dry += (core / 100f) * config.hotDryBonus;
        else if (core < TemperatureHelper.HYPOTHERMIA_DEV) dry *= config.coldDryMultiplier;
        return dry;
    }

    /**
     * Returns 0..1 representing how much of the player's hitbox is filled with water.
     * 0 = dry / toe-tip in nothing, ~0.3 = knees, ~0.5 = waist, 1.0 = head underwater.
     * Uses Minecraft's own {@code getFluidHeight} which is already the per-entity
     * AABB integration it uses for buoyancy — no extra block scanning needed.
     */
    public static float computeSubmersion(Player player) {
        float bbH = player.getBbHeight();
        if (bbH <= 0) return 0f;
        double fluidH = player.getFluidHeight(FluidTags.WATER);
        return (float) Math.min(fluidH / bbH, 1.0);
    }

    public static int computeWaterDepth(Player player, int maxDepth) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        int depth = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < maxDepth; dy++) {
            cursor.set(origin.getX(), origin.getY() + dy, origin.getZ());
            if (!level.getFluidState(cursor).is(FluidTags.WATER)) break;
            depth++;
        }
        return depth;
    }

    /**
     * Shrink value toward 0 by amount, without crossing zero.
     */
    private static float shrink(float value, float amount) {
        if (value > 0) return Math.max(0f, value - amount);
        if (value < 0) return Math.min(0f, value + amount);
        return 0f;
    }

    @Override
    public void readData(ValueInput input) {
        coreTemp = input.getFloatOr(CORE_TEMP_TAG, 0f);
        worldTemp = input.getFloatOr(WORLD_TEMP_TAG, 0f);
        baseOffset = input.getFloatOr(BASE_OFFSET_TAG, 0f);
        baseOffsetTicks = input.getIntOr(BASE_OFFSET_TICKS_TAG, 0);
        wetness = input.getFloatOr(WETNESS_TAG, 0f);
        waterTempAccum = input.getFloatOr(WATER_TEMP_ACCUM_TAG, 0f);
        graceTicks = input.getIntOr(GRACE_TICKS_TAG, 0);
        adaptedBiomeTemp = input.getFloatOr(ADAPTED_BIOME_TEMP_TAG, NEUTRAL_TEMP);
        adaptExposureTicks = input.getIntOr(ADAPT_EXPOSURE_TICKS_TAG, 0);
        lastBiomeBase = adaptedBiomeTemp;
        lastCoreTemp = coreTemp;
    }

    @Override
    public void writeData(ValueOutput output) {
        output.putFloat(CORE_TEMP_TAG, coreTemp);
        output.putFloat(WORLD_TEMP_TAG, worldTemp);
        output.putFloat(BASE_OFFSET_TAG, baseOffset);
        output.putInt(BASE_OFFSET_TICKS_TAG, baseOffsetTicks);
        output.putFloat(WETNESS_TAG, wetness);
        output.putFloat(WATER_TEMP_ACCUM_TAG, waterTempAccum);
        output.putInt(GRACE_TICKS_TAG, graceTicks);
        output.putFloat(ADAPTED_BIOME_TEMP_TAG, adaptedBiomeTemp);
        output.putInt(ADAPT_EXPOSURE_TICKS_TAG, adaptExposureTicks);
    }
}
