package com.sypztep.api;

import com.sypztep.system.temperature.TemperatureHelper.TempZone;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

public final class TemperatureEvents {
    private TemperatureEvents() {}

    /**
     * Fired server-side when a player's temperature zone changes.
     */
    public static final Event<ZoneChanged> ZONE_CHANGED =
            EventFactory.createArrayBacked(ZoneChanged.class,
                    listeners -> (player, oldZone, newZone) -> {
                        for (var l : listeners) l.onZoneChanged(player, oldZone, newZone);
                    });

    /**
     * Fired server-side before temperature damage is dealt. Return {@code false} to cancel.
     */
    public static final Event<BeforeDamage> BEFORE_DAMAGE =
            EventFactory.createArrayBacked(BeforeDamage.class,
                    listeners -> (player, zone, damage) -> {
                        for (var l : listeners) {
                            if (!l.beforeDamage(player, zone, damage)) return false;
                        }
                        return true;
                    });

    @FunctionalInterface
    public interface ZoneChanged {
        void onZoneChanged(Player player, TempZone oldZone, TempZone newZone);
    }

    @FunctionalInterface
    public interface BeforeDamage {
        boolean beforeDamage(Player player, TempZone zone, float damage);
    }
}
