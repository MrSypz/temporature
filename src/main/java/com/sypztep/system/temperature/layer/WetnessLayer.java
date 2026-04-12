package com.sypztep.system.temperature.layer;

import com.sypztep.Temporature;
import com.sypztep.common.PlayerTemperatureComponent;
import com.sypztep.common.TemperatureEntityComponents;
import com.sypztep.system.temperature.WorldHelper;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WetnessLayer implements WorldTemperatureLayer {
    private static final int SAMPLE_SPACING = 4;
    @Override public Identifier id() { return Temporature.id("wetness"); }
    @Override public float priority() { return 700f; }
    @Override public boolean playerSpecific() { return true; }

    @Override
    public double modify(Player player, double currentTemp) {
        // In water → override worldTemp with water's temperature for this tick.
        // Water is the effective ambient; body accumulator drifts toward it.
        if (player.isInWater() || player.isUnderWater()) {
            return sampleWaterTemp(player);
        }

        PlayerTemperatureComponent comp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player);
        float wetness = comp.getWetness();
        if (wetness <= 0) return currentTemp;

        // Wet but out of water: evaporative cooling (hot) or conductive chill (cold).
        // Values in MC units (matching everything else).
        if (currentTemp > 0) return currentTemp - 0.2 * wetness;   // ~-5°C at full wetness
        else return currentTemp - 0.04 * wetness;                  // ~-1°C at full wetness
    }

    public static double sampleWaterTemp(Player player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        double total = 0;
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos sample = origin.offset(dx * SAMPLE_SPACING, 0, dz * SAMPLE_SPACING);
                Holder<Biome> biome = WorldHelper.getCachedBiome(level, sample);
                WorldHelper.BiomeTemp bt = WorldHelper.getBiomeTemperature(level, biome);
                if (bt.waterTemp().isPresent()) {
                    total += bt.waterTemp().getAsDouble();
                } else {
                    total += (bt.lowTemp() + bt.highTemp()) * 0.5;
                }
                count++;
            }
        }

        return total / count;
    }
}
