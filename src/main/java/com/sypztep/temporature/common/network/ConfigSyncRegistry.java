package com.sypztep.temporature.common.network;

import com.google.gson.Gson;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Universal registry for server→client config sync.
 *
 * <p>Any mod can register a config once; the sync system batches all of them
 * into a single handshake so joining a server with 10 mods costs one freeze,
 * one Hello, one Response, and optionally one Data+Ack — not 10 of each.
 *
 * <h3>Registration (call during mod init, server-side):</h3>
 * <pre>{@code
 * ConfigSyncRegistry.register(
 *     Temporature.MODID,
 *     TemporatureServerConfig::getInstance,
 *     TemporatureServerConfig.class,
 *     cfg -> {
 *         TemporatureServerConfig.applyFrom(cfg);
 *         TemporatureServerConfig.setSyncedFromServer(true);
 *     },
 *     ConfigSyncUtil::syncHashCode
 * );
 * }</pre>
 */
public final class ConfigSyncRegistry {

    private ConfigSyncRegistry() {}

    private static final Gson GSON = new Gson();

    // -------------------------------------------------------------------------
    // Internal entry type — holds everything the sync system needs per namespace
    // -------------------------------------------------------------------------

    private record Entry<T>(
            String namespace,
            Supplier<T> instance,
            Class<T> type,
            Consumer<T> applier,
            ToIntFunction<T> hasher
    ) {
        int currentHash() { return hasher.applyAsInt(instance.get()); }
        String toJson()   { return GSON.toJson(instance.get()); }

        @SuppressWarnings("unchecked")
        static void applyJson(Entry<?> e, String json) {
            Entry<Object> cast = (Entry<Object>) e;
            cast.applier().accept(GSON.fromJson(json, cast.type()));
        }
    }

    // LinkedHashMap preserves insertion order; alphabetical master-hash uses TreeMap below.
    private static final Map<String, Entry<?>> REGISTRY = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Public registration API
    // -------------------------------------------------------------------------

    /**
     * Register a config for sync.  Call once during mod initialization.
     *
     * @param namespace  Unique key (typically mod ID). Used as the map key in all packets.
     * @param instance   Supplier of the live server-side config singleton.
     * @param type       Config class — used by Gson to deserialize on the client.
     * @param applier    How to apply the deserialize value (runs on client render thread).
     * @param hasher     How to hash the config (e.g. {@code ConfigSyncUtil::syncHashCode}).
     */
    public static <T> void register(
            String namespace,
            Supplier<T> instance,
            Class<T> type,
            Consumer<T> applier,
            ToIntFunction<T> hasher) {

        if (REGISTRY.containsKey(namespace))
            throw new IllegalStateException("[ConfigSyncRegistry] Namespace already registered: " + namespace);

        REGISTRY.put(namespace, new Entry<>(namespace, instance, type, applier, hasher));
    }

    // -------------------------------------------------------------------------
    // Server-side helpers
    // -------------------------------------------------------------------------

    /** Returns a snapshot of every registered namespace mapped to its current hash. */
    public static Map<String, Integer> collectHashes() {
        Map<String, Integer> out = new LinkedHashMap<>(REGISTRY.size());
        REGISTRY.forEach((ns, e) -> out.put(ns, e.currentHash()));
        return Collections.unmodifiableMap(out);
    }

    /**
     * Computes a single deterministic master hash across all registered configs.
     * Iterates namespaces in alphabetical order so registration order does not matter.
     */
    public static int masterHash() {
        return masterHash(collectHashes());
    }

    /**
     * Computes the master hash from an already-collected hash map.
     * Both server and client use this overload so the computation is identical.
     */
    public static int masterHash(Map<String, Integer> hashes) {
        int result = 1;
        // TreeMap → alphabetical order → deterministic regardless of insertion order
        for (int h : new TreeMap<>(hashes).values()) result = 31 * result + h;
        return result;
    }

    /**
     * Serializes only the given namespaces to JSON.
     * Unknown namespaces are silently skipped.
     */
    public static Map<String, String> serializeFor(Collection<String> namespaces) {
        Map<String, String> out = new LinkedHashMap<>(namespaces.size());
        for (String ns : namespaces) {
            Entry<?> e = REGISTRY.get(ns);
            if (e != null) out.put(ns, e.toJson());
        }
        return Collections.unmodifiableMap(out);
    }

    // -------------------------------------------------------------------------
    // Client-side helpers
    // -------------------------------------------------------------------------

    /**
     * Applies a batch of namespace → JSON pairs on the client.
     * Unknown namespaces are silently skipped.
     * Runs on the client render thread; callers are responsible for thread safety.
     */
    public static void applyBatch(Map<String, String> data) {
        data.forEach((ns, json) -> {
            Entry<?> e = REGISTRY.get(ns);
            if (e != null) Entry.applyJson(e, json);
        });
    }

    /** Unmodifiable view of all registered namespaces. */
    public static Set<String> namespaces() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}