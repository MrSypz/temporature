package com.sypztep.temporature.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sypztep.temporature.Temporature;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores per-server, per-namespace config caches on the client.
 *
 * <h3>File layout:</h3>
 * <pre>
 *   config/temporature/servers/{md5(serverAddress)}.json
 * </pre>
 *
 * <h3>File format (one file per server, all namespaces inside):</h3>
 * <pre>{@code
 * {
 *   "temporature": { "hash": 12345, "json": "{...config json...}" },
 *   "someothermod": { "hash": 67890, "json": "{...}" }
 * }
 * }</pre>
 *
 * <p>Reading the full file on join is one disk read instead of N reads for N namespaces.
 * Writes replace only the changed namespace entries and flush the whole file.
 *
 */
public final class ServerConfigCache {

    private ServerConfigCache() {}

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, NamespaceEntry>>(){}.getType();

    /** One entry per namespace inside the per-server file. */
    private record NamespaceEntry(int hash, String json) {}

    // In-memory write-through cache to avoid redundant disk reads on the same session.
    private static final Map<String, Map<String, NamespaceEntry>> memoryCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the stored hash for {@code namespace} on this server
     * matches {@code expectedHash}.  Does not deserialise the config.
     */
    public static boolean isValid(String serverAddress, String namespace, int expectedHash) {
        NamespaceEntry entry = readAll(serverAddress).get(namespace);
        return entry != null && entry.hash() == expectedHash;
    }

    /**
     * Returns the cached config JSON for {@code namespace} if the stored hash matches
     * {@code expectedHash}; otherwise {@link Optional#empty()}.
     */
    public static Optional<String> tryLoad(String serverAddress, String namespace, int expectedHash) {
        NamespaceEntry entry = readAll(serverAddress).get(namespace);
        if (entry == null || entry.hash() != expectedHash) return Optional.empty();
        return Optional.of(entry.json());
    }

    /**
     * Saves (or overwrites) one namespace entry in the per-server cache file.
     */
    public static void save(String serverAddress, String namespace, String json, int hash) {
        Map<String, NamespaceEntry> all = new java.util.LinkedHashMap<>(readAll(serverAddress));
        all.put(namespace, new NamespaceEntry(hash, json));
        writeAll(serverAddress, all);
    }

    /**
     * Clears the in-memory cache for {@code serverAddress}.
     * Call on disconnect so subsequent logins re-read from disk.
     */
    public static void evict(String serverAddress) {
        memoryCache.remove(serverAddress);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Map<String, NamespaceEntry> readAll(String serverAddress) {
        return memoryCache.computeIfAbsent(serverAddress, addr -> {
            Path path = cachePath(addr);
            if (!Files.exists(path)) return new java.util.LinkedHashMap<>();
            try {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                Map<String, NamespaceEntry> parsed = GSON.fromJson(raw, MAP_TYPE);
                return parsed != null ? parsed : new java.util.LinkedHashMap<>();
            } catch (IOException e) {
                Temporature.LOGGER.warn("Failed to read config cache for '{}': {}", addr, e.getMessage());
                return new java.util.LinkedHashMap<>();
            } catch (Exception e) {
                // Catches JsonSyntaxException / IllegalStateException from old cache formats.
                // Treating as a cache miss is safe: the server will send fresh data (slow path)
                // and writeAll() will overwrite the file with the new format.
                Temporature.LOGGER.warn(
                        "Config cache for '{}' is in an unreadable format, resetting. Cause: {}",
                        addr, e.getMessage());
                deleteSilently(path);
                return new java.util.LinkedHashMap<>();
            }
        });
    }

    private static void deleteSilently(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException ignored) {}
    }

    private static void writeAll(String serverAddress, Map<String, NamespaceEntry> entries) {
        memoryCache.put(serverAddress, entries);
        Path path = cachePath(serverAddress);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(entries), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Temporature.LOGGER.error("Failed to write config cache for '{}': {}", serverAddress, e.getMessage());
        }
    }

    private static Path cachePath(String serverAddress) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(Temporature.MODID)
                .resolve("servers")
                .resolve(md5(serverAddress) + ".json");
    }

    /**
     * Hashes the server address to a safe 8-char hex filename.
     * Colons and slashes in addresses are illegal on some file systems.
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", bytes[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 not available", e);
        }
    }
}