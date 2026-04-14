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
        String wetLine = "§7Wetness: §f" + wetPct + "%";

        g.text(font, wetLine, cx, cy, 0xFFFFFFFF, true);

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