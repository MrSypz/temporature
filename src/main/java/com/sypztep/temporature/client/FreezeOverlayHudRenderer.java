package com.sypztep.temporature.client;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public final class FreezeOverlayHudRenderer implements HudElement {
    private static final Identifier POWDER_SNOW_OUTLINE = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static float smoothedAlpha = 0f;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;
        PlayerTemperatureComponent temp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player);

        double targetAlpha = getPercent(temp);

        smoothedAlpha = (float) Mth.lerp(0.1 * deltaTracker.getGameTimeDeltaTicks(), smoothedAlpha, targetAlpha);

        if (smoothedAlpha <= 0.001f) return;

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        int color = ARGB.white(smoothedAlpha);

        graphics.blit(RenderPipelines.GUI_TEXTURED, POWDER_SNOW_OUTLINE, 0, 0, 0f, 0f, w, h, w, h, color);
    }

    private double getPercent(PlayerTemperatureComponent temp) {
        double dev = temp.getBodyTemp() + temp.getBaseOffset();
        return Mth.clampedMap(dev, TemperatureHelper.FREEZING_DEV,
                TemperatureHelper.CHILLY_DEV,
                1.0, 0.0);
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, Temporature.id("freeze_overlay"), new FreezeOverlayHudRenderer());
    }
}