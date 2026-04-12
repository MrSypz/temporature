package com.sypztep.system.temperature;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

public final class TemperatureLayerRegistry {

    private record LayerEntry(WorldTemperatureLayer layer, float effectivePriority) {}

    private static final List<LayerEntry> LAYERS = new ArrayList<>();
    private static final Map<WorldTemperatureLayer, int[]> AFTER_COUNTS = new HashMap<>();
    private static final Map<WorldTemperatureLayer, int[]> BEFORE_COUNTS = new HashMap<>();

    private TemperatureLayerRegistry() {}

    public static void init() {
        register(TemperatureLayers.BIOME);
        register(TemperatureLayers.ELEVATION);
        register(TemperatureLayers.BLOCK_SCAN);
        register(TemperatureLayers.WEATHER);
        register(TemperatureLayers.DIMENSION);
        register(TemperatureLayers.STRUCTURE);
        register(TemperatureLayers.WETNESS);
    }

    public static void register(WorldTemperatureLayer layer) {
        addEntry(layer, layer.priority());
    }

    public static void register(WorldTemperatureLayer layer, LayerOrder order) {
        float anchorPriority = getPriority(order.anchor);
        float effectivePriority;
        if (order.after) {
            int count = AFTER_COUNTS.computeIfAbsent(order.anchor, _ -> new int[]{0})[0]++;
            effectivePriority = anchorPriority + 0.1f * (count + 1);
        } else {
            int count = BEFORE_COUNTS.computeIfAbsent(order.anchor, _ -> new int[]{0})[0]++;
            effectivePriority = anchorPriority - 0.1f * (count + 1);
        }
        addEntry(layer, effectivePriority);
    }

    private static float getPriority(WorldTemperatureLayer anchor) {
        for (LayerEntry entry : LAYERS) {
            if (entry.layer() == anchor) return entry.effectivePriority();
        }
        return anchor.priority();
    }

    private static void addEntry(WorldTemperatureLayer layer, float priority) {
        LAYERS.add(new LayerEntry(layer, priority));
        LAYERS.sort(Comparator.comparingDouble(LayerEntry::effectivePriority));
    }

    public static double execute(Player player) {
        Level level = player.level();
        long segKey = WorldHelper.segmentKey(player.blockPosition());

        WorldHelper.beginScope();
        try {
            double temp;
            OptionalDouble cached = WorldHelper.getCachedWorldOnly(level, segKey);
            if (cached.isPresent()) {
                temp = cached.getAsDouble();
            } else {
                temp = 0;
                for (LayerEntry entry : LAYERS) {
                    if (entry.layer().playerSpecific()) continue;
                    temp = entry.layer().modify(player, temp);
                }
                WorldHelper.putCachedWorldOnly(level, segKey, temp);
            }

            for (LayerEntry entry : LAYERS) {
                if (!entry.layer().playerSpecific()) continue;
                temp = entry.layer().modify(player, temp);
            }
            return temp;
        } finally {
            WorldHelper.endScope();
        }
    }
}
