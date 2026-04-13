package com.sypztep.temporature.client.provider;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.data.DimensionTemperatureData;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TemporatureDimensionTemperatureProvider extends FabricDynamicRegistryProvider {

    public TemporatureDimensionTemperatureProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, registries);
    }

    @Override
    protected void configure(HolderLookup.@NonNull Provider registries, Entries entries) {
        entries.add(DimensionTemperatureData.key(Temporature.id("nether_offset")),
                new DimensionTemperatureData(List.of(Level.NETHER), TemperatureHelper.cToMc(10.0), true));

        entries.add(DimensionTemperatureData.key(Temporature.id("end_offset")),
                new DimensionTemperatureData(List.of(Level.END), TemperatureHelper.cToMc(-5.0), true));
    }

    @Override
    public @NotNull String getName() {
        return Temporature.MODID + " Dimension Data";
    }
}
