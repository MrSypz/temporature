package com.sypztep.system.temperature;

import net.minecraft.world.entity.player.Player;

public interface WorldTemperatureLayer {
    double modify(Player player, double currentTemp);

    /**
     * Player-specific layers depend on per-player state (insulation, wetness, body box) and
     * cannot be deduped across players sharing a world segment. World-only layers can.
     */
    default boolean playerSpecific() { return false; }
}