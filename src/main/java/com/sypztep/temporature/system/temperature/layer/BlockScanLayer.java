package com.sypztep.temporature.system.temperature.layer;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.temporature.common.data.BlockTemperatureData;
import com.sypztep.temporature.system.temperature.WorldHelper;
import com.sypztep.temporature.system.temperature.WorldTemperatureLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockScanLayer implements WorldTemperatureLayer {
    private static final int MIN_SCAN_INTERVAL = 5;
    @Override public boolean playerSpecific() { return true; }

    private static final double LOG_K = 0.52;

    // global per-(dim, segment) cache shared across players in the same 8-block cube.
    private static final Map<ResourceKey<Level>, Map<Long, CacheEntry>> CACHE = new ConcurrentHashMap<>();

    @Override
    public double modify(Player player, double currentTemp) {
        int r = TemporatureServerConfig.getInstance().blockScanRadius;
        if (r <= 0) return currentTemp;

        Level level = player.level();
        long gameTime = level.getGameTime();
        int interval = scanIntervalForRadius(r);
        BlockPos origin = player.blockPosition();

        Map<Long, CacheEntry> dimCache = CACHE.computeIfAbsent(level.dimension(), _ -> new ConcurrentHashMap<>());
        long segKey = WorldHelper.segmentKey(origin);
        CacheEntry cached = dimCache.get(segKey);
        boolean shouldRescan = cached == null || gameTime - cached.lastScanTick >= interval;

        double contribution;
        if (shouldRescan) {
            contribution = scanContribution(level, player, origin, r, currentTemp);
            dimCache.put(segKey, new CacheEntry(contribution, gameTime));
        } else {
            contribution = cached.contribution;
        }

        return currentTemp + contribution;
    }

    public static void invalidateDimension(ResourceKey<Level> dim) {
        CACHE.remove(dim);
    }

    private static int scanIntervalForRadius(int radius) {
        if (radius <= 4) return MIN_SCAN_INTERVAL;
        if (radius <= 8) return 10;
        return 20;
    }

    private static double scanContribution(Level level, Player player, BlockPos origin, int r, double currentTemp) {
        AABB playerBox = player.getBoundingBox();
        Registry<BlockTemperatureData> reg = level.registryAccess().lookupOrThrow(Temporature.BLOCK_TEMPERATURES);

        Map<Block, List<BlockTemperatureData>> byBlock = new HashMap<>();
        int maxRange = 0;
        for (BlockTemperatureData entry : reg) {
            if (entry.minAmbient().isPresent() && currentTemp < entry.minAmbient().get()) continue;
            if (entry.maxAmbient().isPresent() && currentTemp > entry.maxAmbient().get()) continue;
            for (Holder<Block> h : entry.blocks()) {
                byBlock.computeIfAbsent(h.value(), _ -> new ArrayList<>()).add(entry);
            }
            if (entry.range() > maxRange) maxRange = entry.range();
        }

        if (byBlock.isEmpty()) return 0;
        int scanR = Math.min(r, maxRange);
        if (scanR <= 0) return 0;

        Map<BlockTemperatureData, Double> totals = new IdentityHashMap<>();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (int dx = -scanR; dx <= scanR; dx++) {
            for (int dy = -scanR; dy <= scanR; dy++) {
                for (int dz = -scanR; dz <= scanR; dz++) {
                    mut.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(mut);
                    if (state.isAir()) continue;

                    List<BlockTemperatureData> matches = byBlock.get(state.getBlock());
                    if (matches == null) continue;

                    Vec3 blockCenter = Vec3.atCenterOf(mut);
                    Vec3 playerClosest = closestOnBox(playerBox, blockCenter);
                    double dist = playerClosest.distanceTo(blockCenter);
                    int solidsBetween = -1;

                    for (BlockTemperatureData entry : matches) {
                        if (dist > entry.range()) continue;
                        if (!matchesPredicates(state, entry)) continue;
                        double cur = totals.getOrDefault(entry, 0.0);
                        double cap = Math.abs(entry.maxEffect());
                        if (Math.abs(cur) >= cap) continue;

                        // fade() linearly blends from full temp at d ≤ range/2 to 0 at d = range.
                        double temp = entry.temperature();
                        double tempToAdd;
                        if (entry.fade()) {
                            double half = entry.range() * 0.5;
                            if (dist <= half)               tempToAdd = temp;
                            else if (dist >= entry.range()) tempToAdd = 0;
                            else {
                                double t = (dist - half) / (entry.range() - half);
                                tempToAdd = temp * (1.0 - t);
                            }
                        } else {
                            tempToAdd = temp;
                        }
                        if (tempToAdd == 0) continue;

                        // Accumulate (linear or log), THEN dampen the delta by wall count.
                        double newTotal;
                        if (entry.logarithmic()) {
                            double sign = Math.signum(cur == 0 ? tempToAdd : cur);
                            double absCur = Math.abs(cur);
                            double inverted = Math.pow(absCur, 1.0 / LOG_K);
                            double addedAbs = Math.abs(tempToAdd);
                            newTotal = sign * Math.pow(inverted + addedAbs, LOG_K);
                        } else {
                            newTotal = cur + tempToAdd;
                        }

                        double delta = newTotal - cur;
                        if (solidsBetween < 0) solidsBetween = countBlockedInRay(level, playerClosest, blockCenter);
                        delta /= (solidsBetween + 1);
                        double next = Math.clamp(cur + delta, -cap, cap);
                        totals.put(entry, next);
                    }
                }
            }
        }

        double sum = 0;
        for (double v : totals.values()) sum += v;
        return sum;
    }

    private static boolean matchesPredicates(BlockState state, BlockTemperatureData entry) {
        if (entry.predicates().isEmpty()) return true;
        for (Map.Entry<String, String> req : entry.predicates().entrySet()) {
            Property<?> prop = state.getBlock().getStateDefinition().getProperty(req.getKey());
            if (prop == null) return false;
            Object value = state.getValue(prop);
            if (!value.toString().equalsIgnoreCase(req.getValue())) return false;
        }
        return true;
    }

    private static Vec3 closestOnBox(AABB box, Vec3 target) {
        return new Vec3(
                Math.clamp(target.x, box.minX, box.maxX),
                Math.clamp(target.y, box.minY, box.maxY),
                Math.clamp(target.z, box.minZ, box.maxZ));
    }

    private static int countBlockedInRay(Level level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        if (dist < 1.0) return 0;

        int steps = Math.max(2, (int) (dist * 2.0));
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        long lastPacked = Long.MIN_VALUE;
        int count = 0;

        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double x = from.x + delta.x * t;
            double y = from.y + delta.y * t;
            double z = from.z + delta.z * t;
            sample.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            long packed = sample.asLong();
            if (packed == lastPacked) continue;
            lastPacked = packed;

            BlockState state = level.getBlockState(sample);
            if (isSpreadBlocked(state)) count++;
        }
        return count;
    }

    private static boolean isSpreadBlocked(BlockState state) {
        if (!state.isSolid()) return false;
        if (state.is(BlockTags.LEAVES)) return false;
        Block block = state.getBlock();
        if (block == Blocks.IRON_BARS) return false;
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == Blocks.TINTED_GLASS) return false;
        return !state.is(BlockTags.DOORS) && !state.is(BlockTags.TRAPDOORS) && !state.is(BlockTags.FENCE_GATES);
    }

    private record CacheEntry(double contribution, long lastScanTick) {}
}
