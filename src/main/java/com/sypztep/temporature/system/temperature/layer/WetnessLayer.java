package com.sypztep.temporature.system.temperature.layer;

import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.system.temperature.WorldTemperatureLayer;
import net.minecraft.world.entity.player.Player;

/**
 * Applies the accumulated water temperature offset to worldTemp.
 * <p>
 * The accumulator in {@link PlayerTemperatureComponent} gradually ramps toward
 * the global {@code defaultWaterTemp} config value while submerged and decays
 * while drying — matching Cold Sweat's behavior where water always applies
 * a uniform cooling offset regardless of biome.
 */
public final class WetnessLayer implements WorldTemperatureLayer {
    @Override public boolean playerSpecific() { return true; }

    @Override
    public double modify(Player player, double currentTemp) {
        PlayerTemperatureComponent comp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player);
        float accum = comp.getWaterTempAccum();
        if (accum == 0 && comp.getWetness() <= 0) return currentTemp;

        return currentTemp + accum;
    }
}
