package com.sypztep.system.temperature.layer;

import com.sypztep.Temporature;
import com.sypztep.common.data.StructureTemperatureData;
import com.sypztep.system.temperature.WorldTemperatureLayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;
import java.util.OptionalDouble;

public final class StructureLayer implements WorldTemperatureLayer {
    @Override
    public double modify(Player player, double currentTemp) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return currentTemp;

        Map<Structure, ?> active = serverLevel.structureManager().getAllStructuresAt(player.blockPosition());
        if (active.isEmpty()) return currentTemp;

        Registry<Structure> structureRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Registry<StructureTemperatureData> reg = serverLevel.registryAccess().lookupOrThrow(Temporature.STRUCTURE_TEMPERATURES);

        double offset = 0;
        OptionalDouble override = OptionalDouble.empty();
        for (Structure s : active.keySet()) {
            ResourceKey<Structure> key = structureRegistry.getResourceKey(s).orElse(null);
            if (key == null) continue;

            for (StructureTemperatureData entry : reg) {
                if (!entry.structures().contains(key)) continue;
                if (entry.isOffset()) offset += entry.temperature();
                else override = OptionalDouble.of(entry.temperature());
            }
        }

        double base = override.isPresent() ? override.getAsDouble() : currentTemp;
        return base + offset;
    }
}
