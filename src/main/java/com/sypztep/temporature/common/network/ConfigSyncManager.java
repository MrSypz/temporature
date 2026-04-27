package com.sypztep.temporature.common.network;

import com.google.gson.Gson;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.TemporatureServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server-side state machine for the config sync handshake.
 *
 * <p>Flow A — fast path (cache hit):
 *   JOIN → freeze → ConfigHelloS2C → ConfigReadyC2S → verify → unfreeze
 *
 * <p>Flow B — slow path (no cache or stale):
 *   JOIN → freeze → ConfigHelloS2C → ConfigRequestC2S → ConfigDataS2C → ConfigAckC2S → verify → unfreeze
 *
 * <p>If the client sends ConfigReadyC2S with a stale hash, the server silently
 * falls through to the slow path without disconnecting the player.
 *
 * <p>A 10-second timeout releases frozen players if the handshake never completes,
 * preventing permanent spectator lock on buggy or slow clients.
 */
public final class ConfigSyncManager {

    private ConfigSyncManager() {}

    private static final Gson GSON = new Gson();
    private static final int TIMEOUT_SECONDS = 10;

    public enum SyncState {
        AWAITING_RESPONSE,  // sent Hello, waiting for Ready or Request
        AWAITING_ACK        // sent Data, waiting for Ack
    }

    private record SyncEntry(GameType realMode, SyncState state) {}

    // ConcurrentHashMap: JOIN events and packet handlers can fire on different threads
    private static final Map<UUID, SyncEntry> pending = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "temporature-sync-timeout");
        t.setDaemon(true);
        return t;
    });

    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        GameType real = player.gameMode.getGameModeForPlayer();
        pending.put(player.getUUID(), new SyncEntry(real, SyncState.AWAITING_RESPONSE));
        player.setGameMode(GameType.SPECTATOR);

        int hash = TemporatureServerConfig.getInstance().hashCode();
        ServerPlayNetworking.send(player, new ConfigHelloS2C(hash));

        scheduleTimeout(server, player);
        Temporature.LOGGER.info("Config sync started for {} (hash={})", player.getName().getString(), hash);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        pending.remove(player.getUUID());
    }

    /** Called when client replies with ConfigReadyC2S (fast path — claims cache hit). */
    public static void onConfigReady(ServerPlayer player, ConfigReadyC2S pkt) {
        SyncEntry entry = pending.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_RESPONSE) return;

        int expected = TemporatureServerConfig.getInstance().hashCode();
        if (pkt.hash() != expected) {
            // Stale cache — don't disconnect, silently upgrade to slow path
            Temporature.LOGGER.info(
                    "Ready hash mismatch from {} (got={} expected={}) — sending fresh config",
                    player.getName().getString(), pkt.hash(), expected);
            pending.put(player.getUUID(), new SyncEntry(entry.realMode(), SyncState.AWAITING_ACK));
            ServerPlayNetworking.send(player, buildDataPacket(expected));
            return;
        }

        unfreeze(player, entry.realMode());
    }

    /** Called when client replies with ConfigRequestC2S (slow path — no valid cache). */
    public static void onConfigRequest(ServerPlayer player, ConfigRequestC2S pkt) {
        SyncEntry entry = pending.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_RESPONSE) return;

        int hash = TemporatureServerConfig.getInstance().hashCode();
        pending.put(player.getUUID(), new SyncEntry(entry.realMode(), SyncState.AWAITING_ACK));
        ServerPlayNetworking.send(player, buildDataPacket(hash));
        Temporature.LOGGER.info("Sent full config to {}", player.getName().getString());
    }

    /** Called when client replies with ConfigAckC2S (slow path — confirms it applied data). */
    public static void onConfigAck(ServerPlayer player, ConfigAckC2S pkt) {
        SyncEntry entry = pending.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_ACK) return;

        int expected = TemporatureServerConfig.getInstance().hashCode();
        if (pkt.hash() != expected) {
            Temporature.LOGGER.warn(
                    "Ack hash mismatch from {} (got={} expected={}) — kicking",
                    player.getName().getString(), pkt.hash(), expected);
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

    private static ConfigDataS2C buildDataPacket(int hash) {
        return new ConfigDataS2C(GSON.toJson(TemporatureServerConfig.getInstance()), hash);
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