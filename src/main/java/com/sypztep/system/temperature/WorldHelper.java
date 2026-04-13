package com.sypztep.system.temperature;

import com.sypztep.Temporature;
import com.sypztep.common.data.BiomeTemperatureData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

public final class WorldHelper {
    private WorldHelper() {}

    public record BiomeTemp(double lowTemp, double highTemp) {
        public double blend(double todCos) {
            double mid = (lowTemp + highTemp) * 0.5;
            double amp = (highTemp - lowTemp) * 0.5;
            return mid + amp * todCos;
        }
    }

    public static BiomeTemp getBiomeTemperature(Level level, Holder<Biome> biomeHolder) {
        Registry<BiomeTemperatureData> reg = level.registryAccess().lookupOrThrow(Temporature.BIOME_TEMPERATURES);
        for (BiomeTemperatureData entry : reg) {
            if (entry.biomes().contains(biomeHolder)) {
                return new BiomeTemp(entry.lowTemp(), entry.highTemp());
            }
        }
        double mid = fallbackBiomeBase(biomeHolder.value().getBaseTemperature());
        return new BiomeTemp(mid - 10.0, mid + 5.0);
    }

    /**
     * Returns the biome-specific water temperature if defined in the datapack,
     * otherwise falls back to the given default (typically from config).
     */
    public static double getWaterTemp(Level level, Holder<Biome> biomeHolder, double fallback) {
        Registry<BiomeTemperatureData> reg = level.registryAccess().lookupOrThrow(Temporature.BIOME_TEMPERATURES);
        for (BiomeTemperatureData entry : reg) {
            if (entry.biomes().contains(biomeHolder)) {
                return entry.waterTemp().orElse(fallback);
            }
        }
        return fallback;
    }

    public static double fallbackBiomeBase(float base) {
        if (base <= 0.8f) return -32 + (base / 0.8f) * 32;
        return Math.min(base - 0.8f, 1.2f) / 1.2f * 50;
    }

    public static double todCos(Level level) {
        long clockTime = level.getOverworldClockTime() % 24000L;
        double phase = (clockTime - 6000.0) / 24000.0 * 2 * Math.PI;
        return Math.cos(phase);
    }

    // ---- per-execute biome lookup cache (hrottle 26 getBiome calls/tick) ----

    private static final ThreadLocal<Map<Long, Holder<Biome>>> BIOME_SCOPE = ThreadLocal.withInitial(HashMap::new);

    public static void beginScope() {
        BIOME_SCOPE.get().clear();
    }

    public static void endScope() {
        BIOME_SCOPE.get().clear();
    }

    public static Holder<Biome> getCachedBiome(Level level, BlockPos pos) {
        Map<Long, Holder<Biome>> scope = BIOME_SCOPE.get();
        long key = pos.asLong();
        Holder<Biome> hit = scope.get(key);
        if (hit != null) return hit;
        Holder<Biome> fresh = level.getBiome(pos);
        scope.put(key, fresh);
        return fresh;
    }

    // ---- world-only segment cache (dedupe across players sharing 8-block cube) ----

    public static final int SEGMENT_SHIFT = 3;
    public static final long ROUGH_TTL_TICKS = 200L;

    private record Snapshot(double worldOnlyTemp, long expireTick) {}

    private static final int MAX_SEGMENTS_PER_DIM = 4096;

    private static final Map<ResourceKey<Level>, LinkedHashMap<Long, Snapshot>> CACHE = new HashMap<>();

    public static long segmentKey(BlockPos pos) {
        return BlockPos.asLong(pos.getX() >> SEGMENT_SHIFT, pos.getY() >> SEGMENT_SHIFT, pos.getZ() >> SEGMENT_SHIFT);
    }

    public static synchronized OptionalDouble getCachedWorldOnly(Level level, long segmentKey) {
        LinkedHashMap<Long, Snapshot> dim = CACHE.get(level.dimension());
        if (dim == null) return OptionalDouble.empty();
        Snapshot s = dim.get(segmentKey);
        if (s == null) return OptionalDouble.empty();
        if (level.getGameTime() >= s.expireTick) {
            dim.remove(segmentKey);
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(s.worldOnlyTemp);
    }

    public static synchronized void putCachedWorldOnly(Level level, long segmentKey, double worldOnlyTemp) {
        LinkedHashMap<Long, Snapshot> dim = CACHE.computeIfAbsent(level.dimension(),
                _ -> new LinkedHashMap<>(64, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, Snapshot> eldest) {
                        return size() > MAX_SEGMENTS_PER_DIM;
                    }
                });
        dim.put(segmentKey, new Snapshot(worldOnlyTemp, level.getGameTime() + ROUGH_TTL_TICKS));
    }

    public static synchronized void invalidateDimension(ResourceKey<Level> dim) {
        CACHE.remove(dim);
    }
}
