package com.sypztep.temporature.client;

import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import com.sypztep.temporature.system.temperature.WorldHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class TestScreen extends Screen {
    private static final int ROW_H = 14;

    // Smooth display state
    private float displayedBodyTemp;
    private float displayedWorldTemp;
    private float displayedWetness;
    private float displayedAdapted;
    private boolean initialized = false;

    // Tooltip
    private int tooltipX, tooltipY;

    public TestScreen() {
        super(Component.empty());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (minecraft.player == null) return;

        Font font = minecraft.font;

        int cx = this.width / 2 - 100;
        int cy = this.height / 2 - 40;
        int cw = 200;

        List<Component> tooltipLines = null;

        PlayerTemperatureComponent tempComp =
                TemperatureEntityComponents.PLAYER_TEMPERATURE.get(minecraft.player);

        float targetBodyT = tempComp.getBodyTemp() + tempComp.getBaseOffset();
        float targetWorldT = tempComp.getWorldTemp();
        float targetWetness = tempComp.getWetness();
        float targetAdapted = tempComp.getAdaptedBiomeTemp();
        // Smooth
        if (!initialized) {
            displayedBodyTemp = targetBodyT;
            displayedWorldTemp = targetWorldT;
            displayedWetness = targetWetness;
            displayedAdapted = targetAdapted;
            initialized = true;
        } else {
            float alpha = computeAlpha(delta);
            displayedBodyTemp = Mth.lerp(alpha, displayedBodyTemp, targetBodyT);
            displayedWorldTemp = Mth.lerp(alpha, displayedWorldTemp, targetWorldT);
            displayedWetness = Mth.lerp(alpha, displayedWetness, targetWetness);
            displayedAdapted = Mth.lerp(alpha, displayedAdapted, targetAdapted);
        }

        // ── BODY TEMP ──
        String bodyLine = bodyTempZone();

        g.text(font, bodyLine, cx, cy, 0xFFFFFFFF, true);

        // Tooltip
        if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ROW_H) {
            tooltipLines = List.of(
                    Component.literal("§eBody Temperature"),
                    Component.literal(String.format("§7Core: §f%+.0f", targetBodyT)),
                    Component.literal(String.format("§7Offset: §f%+.1f", tempComp.getBaseOffset()))
            );
            tooltipX = mouseX;
            tooltipY = mouseY;
        }

        cy += ROW_H;

        // ── WORLD TEMP ──
        double worldC = TemperatureHelper.mcToC(displayedWorldTemp);
        String worldLine = String.format("§7World: §f%.1f°C", worldC);

        g.text(font, worldLine, cx, cy, 0xFFFFFFFF, true);

        cy += ROW_H;

        // ── WETNESS ──
        int wetPct = Math.round(displayedWetness * 100);
        float accum = tempComp.getWaterTempAccum();
        double accumC = TemperatureHelper.mcToC(accum);
        String wetLine = String.format("§7Wetness: §f%d%% §7(skin: §f%.1fC§7)", wetPct, accumC);

        g.text(font, wetLine, cx, cy, 0xFFFFFFFF, true);

        if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ROW_H) {
            TemporatureServerConfig wcfg = TemporatureServerConfig.getInstance();
            boolean inWater = minecraft.player.isInWater() || minecraft.player.isUnderWater();
            boolean inRain = !inWater && minecraft.player.level().isRainingAt(minecraft.player.blockPosition());
            Holder<Biome> wBiome = minecraft.player.level().getBiome(minecraft.player.blockPosition());

            java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.literal("§bWater & Wetness"));
            lines.add(Component.literal(String.format("§7Accum: §f%.3f MC §7(%.1fC)", accum, accumC)));

            String stateLine = inWater ? "§9Submerged" : inRain ? "§3In rain" : (targetWetness > 0 ? "§7Drying (residual)" : "§8Dry");
            lines.add(Component.literal("§7State: " + stateLine));

            if (inWater) {
                float submersion = PlayerTemperatureComponent.computeSubmersion(minecraft.player);
                String bodyPart = submersion >= 0.95f ? "§9fully submerged"
                        : submersion >= 0.7f ? "§bshoulders"
                        : submersion >= 0.45f ? "§bwaist"
                        : submersion >= 0.2f ? "§3knees"
                        : "§3tip-toe";
                int depth = PlayerTemperatureComponent.computeWaterDepth(minecraft.player, wcfg.maxWaterDepth);
                double depthT = Math.min(depth / (double) wcfg.maxWaterDepth, 1.0);
                double biomeWater = WorldHelper.getWaterTemp(minecraft.player.level(), wBiome, wcfg.defaultWaterTemp);
                double target = biomeWater + (wcfg.deepWaterTemp - biomeWater) * depthT;
                double fluidH = minecraft.player.getFluidHeight(net.minecraft.tags.FluidTags.WATER);
                float bbH = minecraft.player.getBbHeight();
                lines.add(Component.literal(String.format("§7Submersion: §f%.0f%% %s §8(%.2f/%.2f bb)", submersion * 100, bodyPart, fluidH, bbH)));
                lines.add(Component.literal(String.format("§7Depth: §f%d §7/ %d §8(%.0f%%)", depth, wcfg.maxWaterDepth, depthT * 100)));
                lines.add(Component.literal(String.format("§7Surface water: §f%.2f MC §7(%.1fC)", biomeWater, TemperatureHelper.mcToC(biomeWater))));
                lines.add(Component.literal(String.format("§7Deep water: §f%.2f MC §7(%.1fC)", wcfg.deepWaterTemp, TemperatureHelper.mcToC(wcfg.deepWaterTemp))));
                lines.add(Component.literal(String.format("§7Target: §f%.2f MC §7(%.1fC)", target, TemperatureHelper.mcToC(target))));
                lines.add(Component.literal(String.format("§7Soak rate: §f%.5f/tick §8(%.4f x %.0f%%)", wcfg.waterSoakSpeed * submersion, wcfg.waterSoakSpeed, submersion * 100)));
            } else if (inRain) {
                double rainT = WorldHelper.getRainWaterTemp(minecraft.player.level(), wBiome);
                lines.add(Component.literal(String.format("§7Rain target: §f%.2f MC §7(%.1fC)", rainT, TemperatureHelper.mcToC(rainT))));
                lines.add(Component.literal(String.format("§7Rain cap: §f%.0f%%", wcfg.maxRainWetness * 100)));
                lines.add(Component.literal(String.format("§7Soak rate: §f%.4f/tick", wcfg.rainSoakSpeed)));
            } else if (targetWetness > 0) {
                lines.add(Component.literal(String.format("§7Drift toward world: §f%.2f MC", targetWorldT)));
                lines.add(Component.literal(String.format("§7Drift rate: §f%.4f/tick", wcfg.residualWaterDriftRate)));
                lines.add(Component.literal(String.format("§7Dry rate: §f%.4f §7(+hot: %.4f)", wcfg.dryRate, wcfg.hotDryBonus)));
            }

            lines.add(Component.literal(String.format("§8Blended temp: §f%.2f MC", targetWorldT + (accum - targetWorldT) * targetWetness)));

            tooltipLines = lines;
            tooltipX = mouseX;
            tooltipY = mouseY;
        }

        cy += ROW_H;

        // ── ADAPTATION ──
        TemporatureServerConfig cfg = TemporatureServerConfig.getInstance();
        float neutral = PlayerTemperatureComponent.NEUTRAL_TEMP;
        float adaptShift = displayedAdapted - neutral;
        double adaptedC = TemperatureHelper.mcToC(displayedAdapted);

        Holder<Biome> biome = minecraft.player.level().getBiome(minecraft.player.blockPosition());
        WorldHelper.BiomeTemp bt = WorldHelper.getBiomeTemperature(minecraft.player.level(), biome);
        float biomeBase = (float) ((bt.lowTemp() + bt.highTemp()) * cfg.threshHoldExtreme);
        double biomeBaseC = TemperatureHelper.mcToC(biomeBase);

        float strengthPct = cfg.maxAdaptShift > 0
                ? Math.min(Math.abs(adaptShift) / cfg.maxAdaptShift, 1.0f) * 100f
                : 0f;

        String adaptLabel = Math.abs(adaptShift) < 0.005f ? "§7[Neutral]"
                : adaptShift > 0 ? "§c[Heat-adapted]"
                : "§9[Cold-adapted]";

        String adaptLine = String.format("§7Adapted: §f%.2fC (%+.2f MC) %s §8%.0f%%",
                adaptedC, adaptShift, adaptLabel, strengthPct);
        g.text(font, adaptLine, cx, cy, 0xFFFFFFFF, true);

        if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ROW_H) {
            boolean outdoors = minecraft.player.level().canSeeSky(minecraft.player.blockPosition());
            int exposure = tempComp.getAdaptExposureTicks();
            boolean sustained = exposure >= PlayerTemperatureComponent.SUSTAINED_THRESHOLD;
            float effectiveMin = (float) cfg.minHabitableTemp + adaptShift;
            float effectiveMax = (float) cfg.maxHabitableTemp + adaptShift;

            tooltipLines = List.of(
                    Component.literal("§eBiome Adaptation"),
                    Component.literal(String.format("§7Target biome: §f%.1fC", biomeBaseC)),
                    Component.literal(String.format("§7Drift: §f%+.3f MC", biomeBase - displayedAdapted)),
                    Component.literal(String.format("§7Exposure: §f%d §7ticks (%.1f days)", exposure, exposure / 24000.0)),
                    Component.literal(sustained ? "§aSustained bonus ACTIVE (2x rate)" : "§8Sustained bonus inactive"),
                    Component.literal(outdoors ? "§aOutdoors (ticking)" : "§cIndoors (paused)"),
                    Component.literal(String.format("§7Effective band: §f%.2f §7to §f%.2f MC", effectiveMin, effectiveMax)),
                    Component.literal(String.format("§7Rate: §f%.5f", cfg.adaptRate)),
                    Component.literal(String.format("§7Max shift: §f%.2f §7- Strength: §f%.0f%%", cfg.maxAdaptShift, cfg.adaptStrength * 100))
            );
            tooltipX = mouseX;
            tooltipY = mouseY;
        }

        // Render tooltip
        if (tooltipLines != null) {
            g.setTooltipForNextFrame(font, tooltipLines, Optional.empty(), tooltipX, tooltipY);
        }
    }

    private @NonNull String bodyTempZone() {
        TemperatureHelper.TempZone zone = TemperatureHelper.zoneFor(displayedBodyTemp);

        String zoneLabel = switch (zone) {
            case FREEZING -> "§1[Freezing]";
            case HYPOTHERMIA -> "§9[Hypothermia]";
            case CHILLY -> "§b[Chilly]";
            case NORMAL -> "§a[Normal]";
            case WARM -> "§e[Warm]";
            case HEATSTROKE -> "§6[Heatstroke]";
            case BURNING -> "§4[Burning]";
        };

        double bodyC = TemperatureHelper.coreToCelsius(displayedBodyTemp);
        return String.format("§7Body: §f%.1f°C %s", bodyC, zoneLabel);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractBlurredBackground(@NonNull GuiGraphicsExtractor graphics) { }

    private float computeAlpha(float delta) {
        return Mth.clamp(0.1f * delta, 0.02f, 0.3f);
    }
}