package com.sypztep.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sypztep.Temporature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

public record StructureTemperatureData(
        List<ResourceKey<Structure>> structures,
        double temperature,
        boolean isOffset
) {
    public static ResourceKey<StructureTemperatureData> key(Identifier id) {
        return ResourceKey.create(Temporature.STRUCTURE_TEMPERATURES, id);
    }

    public static final Codec<StructureTemperatureData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.STRUCTURE).listOf().fieldOf("structures").forGetter(StructureTemperatureData::structures),
            Codec.DOUBLE.fieldOf("temperature").forGetter(StructureTemperatureData::temperature),
            Codec.BOOL.optionalFieldOf("isOffset", false).forGetter(StructureTemperatureData::isOffset)
    ).apply(i, StructureTemperatureData::new));
}
