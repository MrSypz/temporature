package com.sypztep.temporature.common.network;

import com.sypztep.temporature.Temporature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Client → Server. Fast path reply.
 * Client sends this when its cache hash matches the server's hello hash.
 * Server verifies the hash and unfreezes the player immediately.
 */
public record ConfigReadyC2S(int hash) implements CustomPacketPayload {

    public static final Type<ConfigReadyC2S> ID = new Type<>(Temporature.id("config_ready"));

    public static final StreamCodec<FriendlyByteBuf, ConfigReadyC2S> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ConfigReadyC2S::hash, ConfigReadyC2S::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}