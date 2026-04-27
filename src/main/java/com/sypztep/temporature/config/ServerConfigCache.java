package com.sypztep.temporature.config;

import com.google.gson.Gson;
import com.sypztep.temporature.Temporature;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stores a separate config file per server on the client.
 *
 * <p>Layout: config/{modid}/servers/{serverAddressHash}.json
 *
 * <p>On every join the server sends its config hash. If the client's
 * cached file for that server has the same hash, the config is applied
 * from disk instantly (fast path) and no JSON is sent over the network.
 * If the hash differs or no cache exists, the server sends the full data
 * and the client writes a new cache file (slow path).
 */
public final class ServerConfigCache {

    private ServerConfigCache() {}

    private static final Gson GSON = new Gson();

    private record CacheEntry(int configHash, String serverAddress, TemporatureServerConfig config) {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the cached config if the stored hash matches {@code serverHash}.
     * Returns {@code null} if the file is missing, unreadable, or stale.
     */
    public static TemporatureServerConfig tryLoad(String serverAddress, int serverHash) {
        Path path = cachePath(serverAddress);
        if (!Files.exists(path)) return null;

        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            CacheEntry entry = GSON.fromJson(raw, CacheEntry.class);
            if (entry == null || entry.configHash() != serverHash) return null;
            return entry.config();
        } catch (IOException e) {
            Temporature.LOGGER.warn("Failed to read config cache for '{}': {}", serverAddress, e.getMessage());
            return null;
        }
    }

    /**
     * Writes (or overwrites) the per-server cache file.
     */
    public static void save(String serverAddress, TemporatureServerConfig config, int hash) {
        Path path = cachePath(serverAddress);
        try {
            Files.createDirectories(path.getParent());
            CacheEntry entry = new CacheEntry(hash, serverAddress, config);
            Files.writeString(path, GSON.toJson(entry), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Temporature.LOGGER.error("Failed to write config cache for '{}': {}", serverAddress, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Path cachePath(String serverAddress) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(Temporature.MODID)
                .resolve("servers")
                .resolve(md5(serverAddress) + ".json");
    }

    /**
     * Hashes the server address to a safe 8-char filename.
     * Server addresses can contain colons and slashes which are illegal in file paths.
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 not available", e);
        }
    }
}