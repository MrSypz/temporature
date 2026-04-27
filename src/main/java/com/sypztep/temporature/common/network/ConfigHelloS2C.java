package com.sypztep.temporature.common.network;

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
 * Server → Client. First handshake message. Sends only the config hash.
 * Client checks its local cache and replies with either ConfigReadyC2S or ConfigRequestC2S.
 */
public record ConfigHelloS2C(int hash) implements CustomPacketPayload {

    public static final Type<ConfigHelloS2C> ID = new Type<>(Temporature.id("config_hello"));

    public static final StreamCodec<FriendlyByteBuf, ConfigHelloS2C> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ConfigHelloS2C::hash, ConfigHelloS2C::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<ConfigHelloS2C> {
        @Override
        public void receive(ConfigHelloS2C payload, ClientPlayNetworking.Context context) {
            String addr = Objects.requireNonNull(context.client().getCurrentServer()).ip;
            TemporatureServerConfig cached = ServerConfigCache.tryLoad(addr, payload.hash());

            context.client().execute(() -> {
                if (cached != null) {
                    // Fast path: valid cache exists with matching hash
                    TemporatureServerConfig.applyFrom(cached);
                    TemporatureServerConfig.setSyncedFromServer(true);
                    ClientPlayNetworking.send(new ConfigReadyC2S(payload.hash()));
                } else {
                    // Slow path: no cache or stale — ask server for full data
                    ClientPlayNetworking.send(new ConfigRequestC2S());
                }
            });
        }
    }
}