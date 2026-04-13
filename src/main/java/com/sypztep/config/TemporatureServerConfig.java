package com.sypztep.config;

import com.sypztep.Temporature;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class TemporatureServerConfig {
    public TemporatureServerConfig() {}
    public static TemporatureServerConfig getInstance() {
        return HANDLER.instance();
    }
    public static final ConfigClassHandler<TemporatureServerConfig> HANDLER = ConfigClassHandler.createBuilder(TemporatureServerConfig.class)
            .id(Temporature.id("config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("temporature_server.json5"))
                    .setJson5(true)
                    .build())
            .build();

    // --- Core Temperature ---

    @SerialEntry
    public boolean enableTemperatureSystem = true;

    @SerialEntry
    public double minHabitableTemp = 0.4;

    @SerialEntry
    public double maxHabitableTemp = 1.5;

    @SerialEntry
    public float tempRate = 1.0f;

    @SerialEntry
    public int tempDamageInterval = 40;

    @SerialEntry
    public float tempBaseDamage = 0.02f;

    @SerialEntry
    public int blockScanRadius = 7;

    // --- Wetness ---

    @SerialEntry
    public float waterSoakSpeed = 0.02f;

    @SerialEntry
    public float rainSoakSpeed = 0.005f;

    @SerialEntry
    public float maxRainWetness = 1.0f;

    @SerialEntry
    public float dryRate = 0.0008f;

    @SerialEntry
    public float hotDryBonus = 0.0008f;

    @SerialEntry
    public float coldDryMultiplier = 0.3f;

    @SerialEntry
    public double defaultWaterTemp = -0.93;
}
