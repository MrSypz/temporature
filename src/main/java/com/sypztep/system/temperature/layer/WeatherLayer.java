package com.sypztep.system.temperature.layer;

import com.sypztep.system.temperature.TemperatureHelper;
import com.sypztep.system.temperature.WorldHelper;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WeatherLayer implements WorldTemperatureLayer {
    private static final int
            RAIN_DELTA = -15,
            THUNDER_DELTA = -25,
            COLD_RAIN_DELTA = -10;

    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        if (!level.canSeeSky(player.blockPosition())) return currentTemp;

        Biome biome = WorldHelper.getCachedBiome(level, player.blockPosition()).value();
        if (!biome.hasPrecipitation()) return currentTemp;

        boolean coldBiome = biome.getBaseTemperature() < TemperatureHelper.cToMc(3.75f);
        if (level.isRaining()) {
            if (coldBiome) return currentTemp + COLD_RAIN_DELTA;
            if (level.isThundering()) return currentTemp + TemperatureHelper.cToMc(THUNDER_DELTA);
            return currentTemp + TemperatureHelper.cToMc(RAIN_DELTA);
        }
        return currentTemp;
    }
}
