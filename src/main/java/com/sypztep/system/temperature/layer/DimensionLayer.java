package com.sypztep.system.temperature.layer;

import com.sypztep.Temporature;
import com.sypztep.common.data.DimensionTemperatureData;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.OptionalDouble;

public final class DimensionLayer implements WorldTemperatureLayer {
    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        ResourceKey<Level> dimKey = level.dimension();
        Registry<DimensionTemperatureData> reg = level.registryAccess().lookupOrThrow(Temporature.DIMENSION_TEMPERATURES);

        double offset = 0;
        OptionalDouble override = OptionalDouble.empty();
        for (DimensionTemperatureData entry : reg) {
            if (!entry.dimensions().contains(dimKey)) continue;
            if (entry.isOffset()) offset += entry.temperature();
            else override = OptionalDouble.of(entry.temperature());
        }

        double base = override.isPresent() ? override.getAsDouble() : currentTemp;
        return base + offset;
    }
}
