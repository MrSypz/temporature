package com.sypztep.temporature.common.network.payload;

import com.sypztep.temporature.Temporature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * <p>Sent after the client successfully applies {@link SyncDataS2C}.
 * Echoes the server's {@code masterHash}; the server verifies it and unfreezes the player.
 */
public record SyncAckC2S(int masterHash) implements CustomPacketPayload {

    public static final Type<SyncAckC2S> ID = new Type<>(Temporature.id("sync_ack"));

    public static final StreamCodec<FriendlyByteBuf, SyncAckC2S> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncAckC2S::masterHash, SyncAckC2S::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}
