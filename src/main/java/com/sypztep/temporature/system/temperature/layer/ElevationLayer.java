package com.sypztep.temporature.system.temperature.layer;

import com.sypztep.temporature.system.temperature.TemperatureHelper;
import com.sypztep.temporature.system.temperature.WorldTemperatureLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Elevation-based temperature modifier depth region model.
 * <p>
 * Five zones from top to bottom:
 * <ol>
 *   <li><b>Above world top</b> — extreme cold</li>
 *   <li><b>World top → ground level</b> — linear cooling per block above ground</li>
 *   <li><b>Ground → 20 blocks deep</b> — linear blend from surface biome temp toward cave midpoint (15.6°C)</li>
 *   <li><b>20 blocks deep → world bottom</b> — logarithmic ramp from 15.6°C toward deep earth (32.2°C)</li>
 *   <li><b>Below world bottom</b> — extreme cold</li>
 * </ol>
 */
public final class ElevationLayer implements WorldTemperatureLayer {

    // Cave shallow: 60°F = 15.6°C — stable cave temperature near surface
    private static final double CAVE_SHALLOW_MC = TemperatureHelper.cToMc(15.6);
    // Deep earth: 90°F = 32.2°C — geothermal warmth near bedrock
    private static final double CAVE_DEEP_MC = TemperatureHelper.cToMc(32.2);
    // Extreme cold for space/void (absolute zero)
    private static final double EXTREME_COLD_MC = TemperatureHelper.cToMc(-273.15);
    // Cooling rate above ground: 0.012 MC/block = 0.3°C/block
    private static final double COOLING_PER_BLOCK = 0.012;
    // Max altitude offset cap
    private static final double ALTITUDE_CAP_MC = TemperatureHelper.cToMc(30);
    // Depth below ground level where shallow cave zone ends
    private static final int SHALLOW_DEPTH = 20;

    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        if (level.dimensionType().hasCeiling()) return currentTemp;

        int y = player.blockPosition().getY();
        int worldTop = level.getMaxY();
        int worldBottom = level.getMinY();
        int groundLevel = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                player.blockPosition().getX(), player.blockPosition().getZ());

        // Zone 5: Below world bottom — extreme cold
        if (y <= worldBottom) {
            return EXTREME_COLD_MC;
        }

        // Zone 1: Above world top — extreme cold
        if (y >= worldTop) {
            return EXTREME_COLD_MC;
        }

        // Zone 2: Above ground level — linear cooling with altitude
        if (y >= groundLevel) {
            int blocksAbove = y - groundLevel;
            if (blocksAbove == 0) return currentTemp;
            double offset = -blocksAbove * COOLING_PER_BLOCK;
            return currentTemp + Math.max(offset, -ALTITUDE_CAP_MC);
        }

        // Underground: y < groundLevel
        int depthBelowGround = groundLevel - y;

        // Zone 3: Ground → 20 blocks deep — linear blend toward cave shallow temp
        if (depthBelowGround <= SHALLOW_DEPTH) {
            double fraction = (double) depthBelowGround / SHALLOW_DEPTH;
            return currentTemp + (CAVE_SHALLOW_MC - currentTemp) * fraction;
        }

        // Zone 4: 20 blocks deep → world bottom — logarithmic ramp from shallow to deep
        // Biome temp is fully overridden here — depth dominates
        int maxDepth = groundLevel - worldBottom;
        int depthBeyondShallow = depthBelowGround - SHALLOW_DEPTH;
        int remainingDepth = Math.max(1, maxDepth - SHALLOW_DEPTH);
        // Logarithmic curve: fast initial change, slow approach to deep temp
        double linearFraction = Math.min(1.0, (double) depthBeyondShallow / remainingDepth);
        double logFraction = Math.log1p(linearFraction * 9) / Math.log(10);
        return CAVE_SHALLOW_MC + (CAVE_DEEP_MC - CAVE_SHALLOW_MC) * logFraction;
    }
}
