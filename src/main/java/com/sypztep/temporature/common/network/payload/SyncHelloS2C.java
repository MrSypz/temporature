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
 * <p>Carries the current hash for <em>every</em> registered namespace so the
 * client can decide which configs it already has cached and which it needs.
 * The client replies with {@link SyncResponseC2S}.
 */
public record SyncHelloS2C(Map<String, Integer> hashes) implements CustomPacketPayload {

    public static final Type<SyncHelloS2C> ID = new Type<>(Temporature.id("sync_hello"));

    public static final StreamCodec<FriendlyByteBuf, SyncHelloS2C> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.hashes().size());
                pkt.hashes().forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeVarInt(v);
                });
            },
            buf -> {
                int n = buf.readVarInt();
                Map<String, Integer> map = new LinkedHashMap<>(n);
                for (int i = 0; i < n; i++) map.put(buf.readUtf(), buf.readVarInt());
                return new SyncHelloS2C(Collections.unmodifiableMap(map));
            }
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    /** Server helper — sends the current hashes for all registered namespaces. */
    public static void send(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SyncHelloS2C(ConfigSyncRegistry.collectHashes()));
    }

    // -------------------------------------------------------------------------

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<SyncHelloS2C> {

        @Override
        public void receive(SyncHelloS2C payload, ClientPlayNetworking.@NonNull Context ctx) {
            String addr = Objects.requireNonNull(ctx.client().getCurrentServer()).ip;

            // Determine which namespaces are not cached (or stale) — off the render thread.
            List<String> missing = new ArrayList<>();
            payload.hashes().forEach((ns, serverHash) -> {
                if (!ServerConfigCache.isValid(addr, ns, serverHash)) missing.add(ns);
            });

            ctx.client().execute(() -> {
                if (missing.isEmpty()) {
                    // ── Fast path ──────────────────────────────────────────────
                    // Every namespace has a valid cache entry; apply them all now
                    // and tell the server we're ready with our master hash.
                    Map<String, String> cachedData = new LinkedHashMap<>();
                    payload.hashes().forEach((ns, hash) ->
                            ServerConfigCache.tryLoad(addr, ns, hash)
                                    .ifPresent(json -> cachedData.put(ns, json)));

                    ConfigSyncRegistry.applyBatch(cachedData);

                    int clientMasterHash = ConfigSyncRegistry.masterHash(payload.hashes());
                    ClientPlayNetworking.send(SyncResponseC2S.fast(clientMasterHash));

                } else {
                    // ── Slow path ──────────────────────────────────────────────
                    // At least one namespace is stale; ask the server for those.
                    ClientPlayNetworking.send(SyncResponseC2S.slow(missing));
                }
            });
        }
    }
}
