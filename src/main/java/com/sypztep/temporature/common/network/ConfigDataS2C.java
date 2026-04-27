package com.sypztep.temporature.common.network;

import com.google.gson.Gson;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.ServerConfigCache;
import com.sypztep.temporature.config.TemporatureServerConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Server → Client. Slow path data delivery.
 * Only sent after the server receives ConfigRequestC2S or detects a stale cache hash.
 * Contains the full config JSON. Client applies it, writes the cache, then sends ConfigAckC2S.
 */
public record ConfigDataS2C(String json, int hash) implements CustomPacketPayload {

    private static final Gson GSON = new Gson();

    public static final Type<ConfigDataS2C> ID = new Type<>(Temporature.id("config_data"));

    public static final StreamCodec<FriendlyByteBuf, ConfigDataS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ConfigDataS2C::json,
            ByteBufCodecs.VAR_INT,     ConfigDataS2C::hash,
            ConfigDataS2C::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<ConfigDataS2C> {
        @Override
        public void receive(ConfigDataS2C payload, ClientPlayNetworking.@NonNull Context context) {
            TemporatureServerConfig cfg = GSON.fromJson(payload.json(), TemporatureServerConfig.class);

            if (cfg.hashCode() != payload.hash()) {
                Temporature.LOGGER.warn("ConfigDataS2C hash mismatch — discarding packet");
                return;
            }

            String addr = Objects.requireNonNull(context.client().getCurrentServer()).ip;

            context.client().execute(() -> {
                TemporatureServerConfig.applyFrom(cfg);
                TemporatureServerConfig.setSyncedFromServer(true);
                ServerConfigCache.save(addr, cfg, payload.hash());
                ClientPlayNetworking.send(new ConfigAckC2S(payload.hash()));
            });
        }
    }
}