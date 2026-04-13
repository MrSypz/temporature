package com.sypztep.temporature.client.provider;

import com.sypztep.temporature.Temporature;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.damagesource.DamageType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class TemporatureDamageTypeProvider extends FabricDynamicRegistryProvider{
    public TemporatureDamageTypeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.@NonNull Provider registries, Entries entries) {
        entries.add(Temporature.HYPOTHERMIA, new DamageType("hypothermia", 0.0f));
        entries.add(Temporature.HEATSTROKE, new DamageType("heatstroke", 0.0f));
    }

    @Override
    public @NonNull String getName() {
        return Temporature.MODID + " Damage Types";
    }
}
