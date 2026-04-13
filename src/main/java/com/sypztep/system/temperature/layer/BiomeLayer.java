package com.sypztep.system.temperature.layer;

import com.sypztep.system.temperature.WorldHelper;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public final class BiomeLayer implements WorldTemperatureLayer {
    private static final int SPACING = 10;

    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();

        double totalLow = 0;
        double totalHigh = 0;
        int count = 0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos sample = origin.offset(dx * SPACING, 0, dz * SPACING);
                Holder<Biome> biome = WorldHelper.getCachedBiome(level, sample);
                WorldHelper.BiomeTemp bt = WorldHelper.getBiomeTemperature(level, biome);
                totalLow += bt.lowTemp();
                totalHigh += bt.highTemp();
                count++;
            }
        }

        double avgLow = totalLow / count;
        double avgHigh = totalHigh / count;
        double mid = (avgLow + avgHigh) * 0.5;
        double amp = (avgHigh - avgLow) * 0.5;

        int skyLight = level.getBrightness(LightLayer.SKY, origin);
        double skylightFactor = skyLight / 15.0;
        double todCos = WorldHelper.todCos(level) * skylightFactor;

        return currentTemp + mid + amp * todCos;
    }
}
