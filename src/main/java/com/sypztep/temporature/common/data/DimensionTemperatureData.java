package com.sypztep.temporature.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sypztep.temporature.Temporature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record DimensionTemperatureData(
        List<ResourceKey<Level>> dimensions,
        double temperature,
        boolean isOffset
) {
    public static ResourceKey<DimensionTemperatureData> key(Identifier id) {
        return ResourceKey.create(Temporature.DIMENSION_TEMPERATURES, id);
    }

    public static final Codec<DimensionTemperatureData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.DIMENSION).listOf().fieldOf("dimensions").forGetter(DimensionTemperatureData::dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(DimensionTemperatureData::temperature),
            Codec.BOOL.optionalFieldOf("isOffset", false).forGetter(DimensionTemperatureData::isOffset)
    ).apply(i, DimensionTemperatureData::new));
}
