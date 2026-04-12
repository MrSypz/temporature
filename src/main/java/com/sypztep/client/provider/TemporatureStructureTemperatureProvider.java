package com.sypztep.client.provider;

import com.sypztep.Temporature;
import com.sypztep.common.data.StructureTemperatureData;
import com.sypztep.system.temperature.TemperatureHelper;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TemporatureStructureTemperatureProvider extends FabricDynamicRegistryProvider {
    public TemporatureStructureTemperatureProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, @NonNull Entries entries) {
        HolderLookup.RegistryLookup<Structure> structures = registries.lookupOrThrow(Registries.STRUCTURE);

        addStructure(entries, structures, "igloo", TemperatureHelper.cToMc(-5.0), false, BuiltinStructures.IGLOO);
        addStructure(entries, structures, "desert_pyramid", TemperatureHelper.cToMc(15.0), true, BuiltinStructures.DESERT_PYRAMID);
        addStructure(entries, structures, "nether_fortress", TemperatureHelper.cToMc(5.0), true, BuiltinStructures.FORTRESS);
        addStructure(entries, structures, "ancient_city", TemperatureHelper.cToMc(-3.0), true, BuiltinStructures.ANCIENT_CITY);
        addStructure(entries, structures, "mansion", TemperatureHelper.cToMc(-2.0), true, BuiltinStructures.WOODLAND_MANSION);
        addStructure(entries, structures, "stronghold", TemperatureHelper.cToMc(-2.0), true, BuiltinStructures.STRONGHOLD);
    }

    @SafeVarargs
    private void addStructure(Entries entries, HolderLookup.RegistryLookup<Structure> lookup,
                              String id, double temp, boolean isOffset, ResourceKey<Structure>... keys) {
        entries.add(StructureTemperatureData.key(Temporature.id(id)),
                new StructureTemperatureData(List.of(keys), temp, isOffset));
    }

    @Override
    public @NonNull String getName() {
        return Temporature.MODID + " Structure Temperature Data";
    }
}
