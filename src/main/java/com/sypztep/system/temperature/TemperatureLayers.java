package com.sypztep.system.temperature;


import com.sypztep.system.temperature.layer.*;

public final class TemperatureLayers {
    public static final BiomeLayer BIOME = new BiomeLayer();
    public static final ElevationLayer ELEVATION = new ElevationLayer();
    public static final BlockScanLayer BLOCK_SCAN = new BlockScanLayer();
    public static final WeatherLayer WEATHER = new WeatherLayer();
    public static final DimensionLayer DIMENSION = new DimensionLayer();
    public static final StructureLayer STRUCTURE = new StructureLayer();
    public static final WetnessLayer WETNESS = new WetnessLayer();

    private TemperatureLayers() {}
}
