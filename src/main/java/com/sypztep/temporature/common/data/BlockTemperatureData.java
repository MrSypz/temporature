package com.sypztep.temporature.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sypztep.temporature.Temporature;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;

public record BlockTemperatureData(
        HolderSet<Block> blocks,
        double temperature,
        int range,
        double maxEffect,
        boolean fade,
        boolean logarithmic,
        Optional<Double> minAmbient,
        Optional<Double> maxAmbient,
        Map<String, String> predicates
) {
    public static ResourceKey<BlockTemperatureData> key(Identifier id) {
        return ResourceKey.create(Temporature.BLOCK_TEMPERATURES, id);
    }

    public static final Codec<BlockTemperatureData> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(BlockTemperatureData::blocks),
            Codec.DOUBLE.fieldOf("temperature").forGetter(BlockTemperatureData::temperature),
            Codec.INT.optionalFieldOf("range", 5).forGetter(BlockTemperatureData::range),
            Codec.DOUBLE.optionalFieldOf("maxEffect", 40.0).forGetter(BlockTemperatureData::maxEffect),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(BlockTemperatureData::fade),
            Codec.BOOL.optionalFieldOf("logarithmic", false).forGetter(BlockTemperatureData::logarithmic),
            Codec.DOUBLE.optionalFieldOf("minAmbient").forGetter(BlockTemperatureData::minAmbient),
            Codec.DOUBLE.optionalFieldOf("maxAmbient").forGetter(BlockTemperatureData::maxAmbient),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("predicates", Map.of()).forGetter(BlockTemperatureData::predicates)
    ).apply(i, BlockTemperatureData::new));
}
