package com.sypztep.temporature.client;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.TemporatureClientConfig;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

public final class WorldGaugeHudRenderer implements HudElement {
    private static final Identifier WORLD_GAUGE_BAR   = Temporature.id("hud/world_temp_border");
    private static final Identifier WORLD_GAUGE_METER = Temporature.id("textures/gui/hud/world_temp_meter.png");

    private static final int BORDER_W = 64;
    private static final int BORDER_H = 26;
    private static final int METER_TEX_W = 80;
    private static final int METER_TEX_H = 16;

    // 1 px line + 1 px gap = tick every 2 px -> 20 px per 1 C
    private static final float PX_PER_CELSIUS = 20f;

    private float smoothWorldTemp = 0f;
    private float displayLeftPx = 0f;
    private float lastDelta = 1f;
    private boolean initialized = false;
    private int lastU = Integer.MIN_VALUE;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        TemporatureServerConfig serverConfig = TemporatureServerConfig.getInstance();
        if (!serverConfig.enableTemperatureSystem) return;
        TemporatureClientConfig clientConfig = TemporatureClientConfig.getInstance();
        if (!clientConfig.showWorldGauge) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Font font = mc.font;
        if (player == null) return;

        float worldTemp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player).getWorldTemp();
        float delta = deltaTracker.getGameTimeDeltaTicks();
        lastDelta = delta;
        if (!initialized) {
            smoothWorldTemp = worldTemp;
            float initCelsius = (float) TemperatureHelper.mcToC(worldTemp);
            displayLeftPx = initCelsius * PX_PER_CELSIUS - BORDER_W / 2f + 1;
            initialized = true;
        }
        else smoothWorldTemp = Mth.lerp(Mth.clamp(0.2f * delta, 0.05f, 0.5f), smoothWorldTemp, worldTemp);

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
        float targetPx = tempCelsius * PX_PER_CELSIUS - BORDER_W / 2f + 1;

        float alpha = Mth.clamp(0.35f * lastDelta, 0.1f, 0.7f);
        float prevDisplay = displayLeftPx;
        displayLeftPx = Mth.lerp(alpha, displayLeftPx, targetPx);
        if (Math.abs(displayLeftPx - targetPx) < 0.5f) displayLeftPx = targetPx;

        // Force odd UV phase so the indicator always lands on a tick, not between.
        int u = Math.floorMod((int) Math.floor(displayLeftPx), METER_TEX_W) | 1;

        playTickSound(u, displayLeftPx > prevDisplay, Math.abs(displayLeftPx - targetPx));
        lastU = u;

        int drawn = 0;
        while (drawn < BORDER_W) {
            int seg = Math.min(METER_TEX_W - u, BORDER_W - drawn);
            graphics.blit(RenderPipelines.GUI_TEXTURED, WORLD_GAUGE_METER,
                    posX + drawn, posY,
                    (float) u, 0f,
                    seg, METER_TEX_H,
                    METER_TEX_W, METER_TEX_H);
            drawn += seg;
            u = 0;
        }
    }

    private void playTickSound(int u, boolean rising, float distanceToTarget) {
        if (!TemporatureClientConfig.getInstance().worldGaugeMeterSound) return;
        if (lastU == Integer.MIN_VALUE || u == lastU) return;
        // Skip while still sliding — avoids burst on big jumps (dimension change, biome edge).
        if (distanceToTarget >= 10f) return;
        UISounds.play(SoundEvents.NOTE_BLOCK_HAT.value(), rising ? 2.0f : 1.4f, 0.05f);
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,
                Temporature.id("world_gauge_hud"), new WorldGaugeHudRenderer());
    }
}