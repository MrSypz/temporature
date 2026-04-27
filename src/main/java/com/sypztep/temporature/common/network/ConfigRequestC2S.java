package com.sypztep.temporature.common.network;

import com.sypztep.temporature.Temporature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Client → Server. Slow path request.
 * Client sends this when it has no valid cache for this server.
 * Server replies with ConfigDataS2C containing the full config JSON.
 */
public record ConfigRequestC2S() implements CustomPacketPayload {

    public static final Type<ConfigRequestC2S> ID = new Type<>(Temporature.id("config_request"));

    // No fields — StreamCodec.unit is the correct pattern for zero-field packets
    public static final StreamCodec<FriendlyByteBuf, ConfigRequestC2S> CODEC =
            StreamCodec.unit(new ConfigRequestC2S());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}