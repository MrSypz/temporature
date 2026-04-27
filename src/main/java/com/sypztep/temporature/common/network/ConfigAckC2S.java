package com.sypztep.temporature.common.network;

import com.sypztep.temporature.Temporature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Client → Server. Slow path acknowledgement.
 * Client sends this after successfully applying ConfigDataS2C.
 * Server verifies the echoed hash and unfreezes the player.
 */
public record ConfigAckC2S(int hash) implements CustomPacketPayload {

    public static final Type<ConfigAckC2S> ID = new Type<>(Temporature.id("config_ack"));

    public static final StreamCodec<FriendlyByteBuf, ConfigAckC2S> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ConfigAckC2S::hash, ConfigAckC2S::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}