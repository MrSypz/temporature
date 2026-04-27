package com.sypztep.temporature.common.network.payload;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.network.ConfigSyncRegistry;
import com.sypztep.temporature.config.ServerConfigCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * <p>Sent only after the server receives a {@link SyncResponseC2S} that lists one or
 * more missing namespaces (or when the server detects a stale master-hash on the fast path).
 * Contains a map of {@code namespace → configJson} for every requested namespace plus a
 * {@code masterHash} the client must echo back in {@link SyncAckC2S}.
 */
public record SyncDataS2C(Map<String, String> data, int masterHash) implements CustomPacketPayload {

    public static final Type<SyncDataS2C> ID = new Type<>(Temporature.id("sync_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncDataS2C> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.data().size());
                pkt.data().forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeUtf(v);
                });
                buf.writeVarInt(pkt.masterHash());
            },
            buf -> {
                int n = buf.readVarInt();
                Map<String, String> map = new LinkedHashMap<>(n);
                for (int i = 0; i < n; i++) map.put(buf.readUtf(), buf.readUtf());
                int hash = buf.readVarInt();
                return new SyncDataS2C(Collections.unmodifiableMap(map), hash);
            }
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    /** Server helper — serialises the requested namespaces and sends the packet. */
    public static void send(ServerPlayer player, Collection<String> namespaces) {
        Map<String, String> data    = ConfigSyncRegistry.serializeFor(namespaces);
        int                 master  = ConfigSyncRegistry.masterHash();
        ServerPlayNetworking.send(player, new SyncDataS2C(data, master));
    }

    // -------------------------------------------------------------------------

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<SyncDataS2C> {

        @Override
        public void receive(SyncDataS2C payload, ClientPlayNetworking.@NonNull Context ctx) {
            ctx.client().execute(() -> {
                String addr = Objects.requireNonNull(ctx.client().getCurrentServer()).ip;

                // Apply all received configs through the registry.
                ConfigSyncRegistry.applyBatch(payload.data());

                // Persist each namespace to the local cache keyed by its post-apply hash.
                // collectHashes() reflects the just-applied state, so the hashes are fresh.
                Map<String, Integer> nowHashes = ConfigSyncRegistry.collectHashes();
                payload.data().forEach((ns, json) -> {
                    int nsHash = nowHashes.getOrDefault(ns, 0);
                    ServerConfigCache.save(addr, ns, json, nsHash);
                });

                // Validate: the applied master hash must match what the server sent.
                int actualMaster = ConfigSyncRegistry.masterHash();
                if (actualMaster != payload.masterHash()) {
                    Temporature.LOGGER.warn(
                            "SyncDataS2C master-hash mismatch (got={} expected={}) — proceeding anyway",
                            actualMaster, payload.masterHash());
                }

                // Echo the server's master hash so the server can unfreeze us.
                ClientPlayNetworking.send(new SyncAckC2S(payload.masterHash()));
            });
        }
    }
}
