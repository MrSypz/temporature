package com.sypztep.system.temperature.layer;

import com.sypztep.Temporature;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ElevationLayer implements WorldTemperatureLayer {
    @Override public Identifier id() { return Temporature.id("elevation"); }
    @Override public float priority() { return 200f; }

    // Deltas in MC units (1 MC = 25°C).
    // 0.012 MC/block = 0.3°C/block, 0.008 MC/block = 0.2°C/block.
    // Clamp: ±1.2 MC = ±30°C.
    private static final double COOLING_PER_BLOCK_ABOVE = 0.012;
    private static final double WARMING_PER_BLOCK_BELOW = 0.008;
    private static final double CLAMP = 1.2;

    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        if (level.dimensionType().hasCeiling()) return currentTemp;

        int y = player.blockPosition().getY();
        double delta = 0;
        if (y > 100) delta = -(y - 100) * COOLING_PER_BLOCK_ABOVE;
        else if (y < 0) delta = (-y) * WARMING_PER_BLOCK_BELOW;
        delta = Math.clamp(delta, -CLAMP, CLAMP);
        return currentTemp + delta;
    }
}
