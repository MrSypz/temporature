package com.sypztep.temporature.common.network;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.network.payload.SyncAckC2S;
import com.sypztep.temporature.common.network.payload.SyncDataS2C;
import com.sypztep.temporature.common.network.payload.SyncHelloS2C;
import com.sypztep.temporature.common.network.payload.SyncResponseC2S;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server-side state machine for the universal config sync handshake.
 *
 * <h3>Fast path (all caches valid):</h3>
 * <pre>
 *   JOIN → freeze → SyncHelloS2C → SyncResponseC2S(missing=[], hash) → verify → unfreeze
 * </pre>
 *
 * <h3>Slow path (one or more caches stale):</h3>
 * <pre>
 *   JOIN → freeze → SyncHelloS2C → SyncResponseC2S(missing=[…]) → SyncDataS2C → SyncAckC2S → verify → unfreeze
 * </pre>
 *
 * <p>If the fast-path master hash mismatches, the server silently upgrades to the slow
 * path and sends data for <em>all</em> namespaces rather than disconnecting the player.
 *
 * <p>A {@value #TIMEOUT_SECONDS}-second watchdog releases frozen players if the
 * handshake never completes, preventing a permanent spectator lock on buggy clients.
 */
public final class ConfigSyncManager {

    private ConfigSyncManager() {}

    private static final int TIMEOUT_SECONDS = 10;

    public enum SyncState {
        AWAITING_RESPONSE,  // sent Hello, waiting for SyncResponseC2S
        AWAITING_ACK        // sent Data, waiting for SyncAckC2S
    }

    private record SyncEntry(GameType realMode, SyncState state) {}

    private static final Map<UUID, SyncEntry> pending = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService TIMEOUT_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "temporature-sync-timeout");
                t.setDaemon(true);
                return t;
            });

    // -------------------------------------------------------------------------
    // Lifecycle hooks (called from ServerPlayConnectionEvents)
    // -------------------------------------------------------------------------

    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        GameType real = player.gameMode.getGameModeForPlayer();
        pending.put(player.getUUID(), new SyncEntry(real, SyncState.AWAITING_RESPONSE));
        player.setGameMode(GameType.SPECTATOR);

        SyncHelloS2C.send(player);
        scheduleTimeout(server, player);

        Temporature.LOGGER.info(
                "Config sync started for {} (masterHash={})",
                player.getName().getString(), ConfigSyncRegistry.masterHash());
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        pending.remove(player.getUUID());
    }

    // -------------------------------------------------------------------------
    // Packet handlers (called from ServerPlayNetworking receivers)
    // -------------------------------------------------------------------------

    /**
     * Called when client replies with {@link SyncResponseC2S}.
     *
     * <ul>
     *   <li>Fast path — {@code missing} is empty and client sends its master hash.</li>
     *   <li>Slow path — {@code missing} lists which namespaces the client needs.</li>
     * </ul>
     */
    public static void onSyncResponse(ServerPlayer player, SyncResponseC2S pkt) {
        SyncEntry entry = pending.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_RESPONSE) return;

        if (pkt.isFastPath()) {
            int expected = ConfigSyncRegistry.masterHash();

            if (pkt.masterHash() == expected) {
                // ── Fast path: hashes agree → unfreeze immediately ──────────
                unfreeze(player, entry.realMode());
            } else {
                // ── Stale fast path: master hash mismatch → slow-path all ───
                Temporature.LOGGER.info(
                        "Fast-path hash mismatch from {} (got={} expected={}) — sending full config",
                        player.getName().getString(), pkt.masterHash(), expected);
                pending.put(player.getUUID(), new SyncEntry(entry.realMode(), SyncState.AWAITING_ACK));
                SyncDataS2C.send(player, new ArrayList<>(ConfigSyncRegistry.namespaces()));
            }

        } else {
            // ── Slow path: send only what the client asked for ───────────────
            Temporature.LOGGER.info("Sending data for [{}] to {}",
                    String.join(", ", pkt.missing()), player.getName().getString());
            pending.put(player.getUUID(), new SyncEntry(entry.realMode(), SyncState.AWAITING_ACK));
            SyncDataS2C.send(player, pkt.missing());
        }
    }

    /**
     * Called when client replies with {@link SyncAckC2S} (slow path confirmation).
     */
    public static void onSyncAck(ServerPlayer player, SyncAckC2S pkt) {
        SyncEntry entry = pending.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_ACK) return;

        int expected = ConfigSyncRegistry.masterHash();
        if (pkt.masterHash() != expected) {
            Temporature.LOGGER.warn(
                    "Ack master-hash mismatch from {} (got={} expected={}) — kicking",
                    player.getName().getString(), pkt.masterHash(), expected);
            pending.remove(player.getUUID());
            player.connection.disconnect(Component.literal(
                    "[Temporature] Config sync failed. Please rejoin."));
            return;
        }

        unfreeze(player, entry.realMode());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void unfreeze(ServerPlayer player, GameType mode) {
        pending.remove(player.getUUID());
        player.setGameMode(mode);
        Temporature.LOGGER.info(
                "Config sync complete for {}, restored game mode {}",
                player.getName().getString(), mode);
    }

    private static void scheduleTimeout(MinecraftServer server, ServerPlayer player) {
        UUID uuid = player.getUUID();
        TIMEOUT_EXECUTOR.schedule(() -> server.execute(() -> {
            SyncEntry entry = pending.get(uuid);
            if (entry != null) {
                Temporature.LOGGER.warn(
                        "Config sync timeout for {} after {}s — releasing",
                        player.getName().getString(), TIMEOUT_SECONDS);
                unfreeze(player, entry.realMode());
            }
        }), TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}