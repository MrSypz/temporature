package com.sypztep.temporature.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.network.payload.SyncDataS2C;
import com.sypztep.temporature.common.network.payload.SyncHelloS2C;
import com.sypztep.temporature.config.ServerConfigCache;
import com.sypztep.temporature.config.TemporatureClientConfig;
import com.sypztep.temporature.config.TemporatureServerConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class TemporatureClient implements ClientModInitializer {
    public static final KeyMapping.Category SURVIVAL = KeyMapping.Category.register(Temporature.id("survival"));
    public static KeyMapping METABOLISM;

    @Override
    public void onInitializeClient() {
        TemporatureServerConfig.HANDLER.load();
        TemporatureClientConfig.HANDLER.load();

        METABOLISM = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.temporature.metabolism", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, SURVIVAL));
        KeyEvent.register();

        FreezeOverlayHudRenderer.register();
        HeatHazeRenderer.register();
        WorldGaugeHudRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(SyncHelloS2C.ID, new SyncHelloS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(SyncDataS2C.ID,  new SyncDataS2C.Receiver());

        ClientPlayConnectionEvents.DISCONNECT.register((_, client) -> {
            if (TemporatureServerConfig.isSyncedFromServer()) {
                TemporatureServerConfig.HANDLER.load();
                TemporatureServerConfig.setSyncedFromServer(false);
                Temporature.LOGGER.info("Restored local server config after disconnect");
            }

            String addr = client.getCurrentServer() != null
                    ? client.getCurrentServer().ip
                    : null;
            if (addr != null) ServerConfigCache.evict(addr);
        });
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