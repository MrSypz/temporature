package com.sypztep.temporature.api;

import net.minecraft.world.entity.player.Player;

/**
 * Modifies the per-tick core temperature accumulation delta before it is applied.
 * <p>
 * Register via {@link TemporatureApi#registerRateModifier(CoreTempModifier)}.
 * Modifiers are chained in registration order — each receives the previous modifier's output.
 */
@FunctionalInterface
public interface CoreTempModifier {
    /**
     * @param player    the player being evaluated
     * @param changeBy  the computed delta (positive = heating, negative = cooling)
     * @param worldTemp current ambient temperature in MC units
     * @param coreTemp  current core accumulator value (+-150 scale)
     * @return the modified changeBy value
     */
    double modify(Player player, double changeBy, double worldTemp, double coreTemp);
}
