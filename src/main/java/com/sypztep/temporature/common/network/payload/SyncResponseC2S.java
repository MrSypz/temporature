package com.sypztep.temporature.common.network.payload;

import com.sypztep.temporature.Temporature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * <ul>
 *   <li><b>Fast path</b> — {@code missing} is empty; {@code masterHash} is the client's
 *       computed master hash for server verification.</li>
 *   <li><b>Slow path</b> — {@code missing} lists the namespaces the client needs data for;
 *       {@code masterHash} is {@code 0} and ignored by the server.</li>
 * </ul>
 */
public record SyncResponseC2S(List<String> missing, int masterHash) implements CustomPacketPayload {

    public static final Type<SyncResponseC2S> ID = new Type<>(Temporature.id("sync_response"));

    public static final StreamCodec<FriendlyByteBuf, SyncResponseC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncResponseC2S::missing,
            ByteBufCodecs.VAR_INT,                                  SyncResponseC2S::masterHash,
            SyncResponseC2S::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    // ── Factory helpers — use these instead of the constructor ────────────────
    /** Fast-path: client has all caches, sends its computed master hash for verification. */
    public static SyncResponseC2S fast(int masterHash) {
        return new SyncResponseC2S(List.of(), masterHash);
    }

    /** Slow-path: client is missing one or more namespaces. */
    public static SyncResponseC2S slow(List<String> missing) {
        return new SyncResponseC2S(List.copyOf(missing), 0);
    }

    public boolean isFastPath() { return missing.isEmpty(); }

    // ── Client-side send helpers ──────────────────────────────────────────────
    public static void sendFast(int masterHash) {
        ClientPlayNetworking.send(fast(masterHash));
    }

    public static void sendSlow(List<String> missing) {
        ClientPlayNetworking.send(slow(missing));
    }
}
