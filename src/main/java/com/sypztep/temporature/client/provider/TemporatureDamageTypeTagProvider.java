package com.sypztep.temporature.client.provider;

import com.sypztep.temporature.Temporature;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class TemporatureDamageTypeTagProvider extends FabricTagsProvider<DamageType> {
    public TemporatureDamageTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.DAMAGE_TYPE, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider registries) {
        builder(DamageTypeTags.BYPASSES_ARMOR)
                .add(Temporature.HYPOTHERMIA)
                .add(Temporature.HEATSTROKE)
        ;
        builder(DamageTypeTags.BYPASSES_SHIELD)
                .add(Temporature.HYPOTHERMIA)
                .add(Temporature.HEATSTROKE)
        ;

        builder(DamageTypeTags.BYPASSES_ENCHANTMENTS)
                .add(Temporature.HYPOTHERMIA)
                .add(Temporature.HEATSTROKE)
        ;

        builder(DamageTypeTags.NO_IMPACT)
                .add(Temporature.HYPOTHERMIA)
                .add(Temporature.HEATSTROKE)
        ;

        builder(DamageTypeTags.NO_KNOCKBACK)
                .add(Temporature.HYPOTHERMIA)
                .add(Temporature.HEATSTROKE)
        ;
    }
}
