package com.sypztep.temporature.system.temperature.layer;

import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.system.temperature.WorldTemperatureLayer;
import net.minecraft.world.entity.player.Player;

/**
 * Blends ambient worldTemp with the player's water temperature accumulator
 * based on how wet the player is.
 * <p>
 * Fully wet ({@code wetness = 1}) → player feels the water's temperature entirely.
 * Dry → worldTemp unchanged.
 * Partial → linear blend between ambient air and water temp.
 * <p>
 * Water temperature stored in {@code waterTempAccum} is an absolute MC value
 * (biome-specific, depth-adjusted), not a delta.
 */
public final class WetnessLayer implements WorldTemperatureLayer {
    @Override public boolean playerSpecific() { return true; }

    @Override
    public double modify(Player player, double currentTemp) {
        PlayerTemperatureComponent comp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player);
        float wetness = comp.getWetness();
        if (wetness <= 0) return currentTemp;

        float accum = comp.getWaterTempAccum();
        return currentTemp + (accum - currentTemp) * wetness;
    }
}
