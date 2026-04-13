package com.sypztep.temporature.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sypztep.temporature.Temporature;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

public record BiomeTemperatureData(
        HolderSet<Biome> biomes,
        double lowTemp,
        double highTemp,
        Optional<Double> waterTemp
) {
    public static ResourceKey<BiomeTemperatureData> key(Identifier id) {
        return ResourceKey.create(Temporature.BIOME_TEMPERATURES, id);
    }

    public static final Codec<BiomeTemperatureData> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeTemperatureData::biomes),
            Codec.DOUBLE.fieldOf("lowTemp").forGetter(BiomeTemperatureData::lowTemp),
            Codec.DOUBLE.fieldOf("highTemp").forGetter(BiomeTemperatureData::highTemp),
            Codec.DOUBLE.optionalFieldOf("waterTemp").forGetter(BiomeTemperatureData::waterTemp)
    ).apply(i, BiomeTemperatureData::new));
}
