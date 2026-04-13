package com.sypztep.client;

import com.sypztep.Temporature;
import com.sypztep.config.TemporatureClientConfig;
import com.sypztep.common.TemperatureEntityComponents;
import com.sypztep.system.temperature.TemperatureHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

public final class WorldGaugeHudRenderer implements HudElement {
    private static final Identifier WORLD_GAUGE_BAR   = Temporature.id("hud/world_temp_border");
    private static final Identifier WORLD_GAUGE_METER = Temporature.id("textures/gui/hud/world_temp_meter.png");

    private static final int BORDER_W = 64;
    private static final int BORDER_H = 26;

    // Tick layout: 1 px line + 1 px gap = 2 px per 0.1 C  ->  20 px per 1 C
    private static final float PX_PER_CELSIUS = 20f;

    private float smoothWorldTemp = 0f;
    private float prevSmoothWorldTemp = 0f;
    private float displayLeftPx = 0f; // the actual rendered position, lerps toward target
    private boolean initialized   = false;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        TemporatureClientConfig clientConfig = TemporatureClientConfig.getInstance();
        if (!clientConfig.showWorldGauge) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Font font = mc.font;
        if (player == null) return;

        float worldTemp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player).getWorldTemp();
        float delta = deltaTracker.getGameTimeDeltaTicks();
        prevSmoothWorldTemp = smoothWorldTemp;
        if (!initialized) {
            smoothWorldTemp = worldTemp;
            prevSmoothWorldTemp = worldTemp;
            float initCelsius = (float) TemperatureHelper.mcToC(worldTemp);
            displayLeftPx = initCelsius * PX_PER_CELSIUS - BORDER_W / 2f + 1;
            initialized = true;
        }
        else smoothWorldTemp = Mth.lerp(Mth.clamp(0.1f * delta, 0.02f, 0.3f), smoothWorldTemp, worldTemp);

        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int posX = (w / 2) + 93, posY = h - 19;

        renderMeter(graphics, posX, posY);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WORLD_GAUGE_BAR, BORDER_W, BORDER_H,
                0, 0, posX, posY - 10, BORDER_W, BORDER_H);

        String displayTemp = clientConfig.temperatureUnit.format(smoothWorldTemp);
        renderTemperature(graphics, font, posX, posY, displayTemp);
    }
    private void renderTemperature(GuiGraphicsExtractor graphics, Font font, int x, int y, String text) {
        graphics.fill(x + 14, y, x + 50, y - 7, 0x88000000);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 32, y - 7);
        graphics.pose().scale(0.8f, 0.8f);
        graphics.centeredText(font, text, 0, 0, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }

    private void renderMeter(GuiGraphicsExtractor graphics, int posX, int posY) {

        float tempCelsius = (float) TemperatureHelper.mcToC(smoothWorldTemp);

        float centerPx = tempCelsius * PX_PER_CELSIUS;
        float leftPx   = centerPx - BORDER_W / 2f + 1;

        float tempDelta = Math.abs(smoothWorldTemp - prevSmoothWorldTemp);
        boolean settled = tempDelta < 0.00001f;
        float targetPx;
        if (settled) {
            targetPx = (int) Math.floor(leftPx) | 1;
        } else {
            targetPx = leftPx;
        }

        displayLeftPx = Mth.lerp(0.3f, displayLeftPx, targetPx);
        if (Math.abs(displayLeftPx - targetPx) < 0.01f) displayLeftPx = targetPx;
        int meterTexW = 80;
        float u = ((displayLeftPx % meterTexW) + meterTexW) % meterTexW;

        int drawn = 0;
        while (drawn < BORDER_W) {
            int seg = Math.min(Math.round(meterTexW - u), BORDER_W - drawn);
            if (seg <= 0) { u = 0; continue; }

            int meterTexH = 16;
            graphics.blit(RenderPipelines.GUI_TEXTURED, WORLD_GAUGE_METER,
                    posX + drawn, posY,
                    u, 0f,
                    seg, meterTexH,
                    meterTexW, meterTexH);

            drawn += seg;
            u = 0;
        }
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,
                Temporature.id("world_gauge_hud"), new WorldGaugeHudRenderer());
    }
}