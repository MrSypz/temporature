package com.sypztep.system.temperature;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public final class TemperatureLayerRegistry {

    private static final List<WorldTemperatureLayer> LAYERS = new ArrayList<>();

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

    /** Appends a layer to the end of the list. */
    public static void register(WorldTemperatureLayer layer) {
        LAYERS.add(layer);
    }

    /** Inserts a layer relative to an existing anchor layer. */
    public static void register(WorldTemperatureLayer layer, LayerOrder order) {
        int anchorIndex = LAYERS.indexOf(order.anchor);
        if (anchorIndex < 0) {
            LAYERS.add(layer);
            return;
        }
        int insertAt = order.after ? anchorIndex + 1 : anchorIndex;
        LAYERS.add(insertAt, layer);
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
                for (WorldTemperatureLayer layer : LAYERS) {
                    if (layer.playerSpecific()) continue;
                    temp = layer.modify(player, temp);
                }
                WorldHelper.putCachedWorldOnly(level, segKey, temp);
            }

            for (WorldTemperatureLayer layer : LAYERS) {
                if (!layer.playerSpecific()) continue;
                temp = layer.modify(player, temp);
            }
            return temp;
        } finally {
            WorldHelper.endScope();
        }
    }
}
