package com.sypztep.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sypztep.Temporature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class TemporatureClient implements ClientModInitializer {
    public static final KeyMapping.Category SURVIVAL = KeyMapping.Category.register(Temporature.id("survival"));
    public static KeyMapping METABOLISM;

    @Override
    public void onInitializeClient() {
        METABOLISM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.temporature.metabolism", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, SURVIVAL));
        KeyEvent.register();

        FreezeOverlayHudRenderer.register();
        HeatHazeRenderer.register();
    }
    public static class KeyEvent implements ClientTickEvents.EndTick {
        public KeyEvent() {}

        public static void register() {
            ClientTickEvents.END_CLIENT_TICK.register(new KeyEvent());
        }

        @Override
        public void onEndTick(@NonNull Minecraft minecraft) {
            if (METABOLISM.consumeClick()) minecraft.setScreen(new TestScreen());
        }
    }
}
