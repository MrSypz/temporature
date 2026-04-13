package com.sypztep.common.network;

import com.google.gson.Gson;
import com.sypztep.Temporature;
import com.sypztep.client.ConfigSyncScreen;
import com.sypztep.config.TemporatureServerConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record ConfigSyncPayloadS2C(String json, int hash) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<ConfigSyncPayloadS2C> ID = new Type<>(Temporature.id("config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayloadS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ConfigSyncPayloadS2C::json,
            ByteBufCodecs.VAR_INT, ConfigSyncPayloadS2C::hash,
            ConfigSyncPayloadS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }

    public static void send(ServerPlayer player) {
        TemporatureServerConfig cfg = TemporatureServerConfig.getInstance();
        String json = GSON.toJson(cfg);
        ServerPlayNetworking.send(player, new ConfigSyncPayloadS2C(json, cfg.hashCode()));
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<ConfigSyncPayloadS2C> {
        @Override
        public void receive(ConfigSyncPayloadS2C payload, ClientPlayNetworking.Context context) {
            TemporatureServerConfig serverCfg = GSON.fromJson(payload.json(), TemporatureServerConfig.class);
            if (serverCfg.hashCode() != payload.hash()) {
                Temporature.LOGGER.warn("Config sync hash mismatch — packet corrupted?");
                return;
            }
            if (TemporatureServerConfig.isSyncedFromServer()
                    && serverCfg.hashCode() == TemporatureServerConfig.getInstance().hashCode()) {
                return;
            }
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().setScreen(new ConfigSyncScreen(serverCfg, payload.hash())));
        }
    }
}
