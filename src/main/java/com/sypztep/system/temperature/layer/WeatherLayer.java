package com.sypztep.system.temperature.layer;

import com.sypztep.Temporature;
import com.sypztep.system.temperature.WorldHelper;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WeatherLayer implements WorldTemperatureLayer {
    // All deltas in MC units (1 MC = 25°C).
    private static final double COLD_RAIN_DELTA = -0.4;  // -10°C
    private static final double THUNDER_DELTA = -1.0;  // -25°C
    private static final double RAIN_DELTA = -0.6;  // -15°C

    @Override public Identifier id() { return Temporature.id("weather"); }
    @Override public float priority() { return 400f; }

    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        if (!level.canSeeSky(player.blockPosition())) return currentTemp;

        Biome biome = WorldHelper.getCachedBiome(level, player.blockPosition()).value();
        if (!biome.hasPrecipitation()) return currentTemp;

        boolean coldBiome = biome.getBaseTemperature() < 0.15f;
        if (level.isRaining()) {
            if (coldBiome) return currentTemp + COLD_RAIN_DELTA;
            if (level.isThundering()) return currentTemp + THUNDER_DELTA;
            return currentTemp + RAIN_DELTA;
        }
        return currentTemp;
    }
}
