package com.sypztep.api;

import com.sypztep.common.PlayerTemperatureComponent;
import com.sypztep.common.TemperatureEntityComponents;
import com.sypztep.system.temperature.LayerOrder;
import com.sypztep.system.temperature.TemperatureLayerRegistry;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class TemporatureApi {
    private static final List<CoreTempModifier> RATE_MODIFIERS = new ArrayList<>();

    private TemporatureApi() {}

    // --- Rate modifier registry ---

    public static void registerRateModifier(CoreTempModifier modifier) {
        RATE_MODIFIERS.add(modifier);
    }

    /**
     * Apply all registered rate modifiers to the accumulation delta.
     * Called internally by {@link PlayerTemperatureComponent}.
     */
    public static double applyRateModifiers(Player player, double changeBy, double worldTemp, double coreTemp) {
        for (CoreTempModifier mod : RATE_MODIFIERS) {
            changeBy = mod.modify(player, changeBy, worldTemp, coreTemp);
        }
        return changeBy;
    }

    // --- Layer registration ---

    public static void registerLayer(WorldTemperatureLayer layer) {
        TemperatureLayerRegistry.register(layer);
    }

    public static void registerLayer(WorldTemperatureLayer layer, LayerOrder order) {
        TemperatureLayerRegistry.register(layer, order);
    }

    // --- Component access ---

    public static PlayerTemperatureComponent getComponent(Player player) {
        return TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player);
    }
}
