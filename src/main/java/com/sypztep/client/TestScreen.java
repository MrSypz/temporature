package com.sypztep.client;

import com.sypztep.common.PlayerTemperatureComponent;
import com.sypztep.common.TemperatureEntityComponents;
import com.sypztep.system.temperature.TemperatureHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class TestScreen extends Screen {
    private static final int ROW_H = 14;

    // Smooth display state
    private float displayedBodyTemp;
    private float displayedWorldTemp;
    private float displayedWetness;
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
        // Smooth
        if (!initialized) {
            displayedBodyTemp = targetBodyT;
            displayedWorldTemp = targetWorldT;
            displayedWetness = targetWetness;
            initialized = true;
        } else {
            float alpha = computeAlpha(delta);
            displayedBodyTemp = Mth.lerp(alpha, displayedBodyTemp, targetBodyT);
            displayedWorldTemp = Mth.lerp(alpha, displayedWorldTemp, targetWorldT);
            displayedWetness = Mth.lerp(alpha, displayedWetness, targetWetness);
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