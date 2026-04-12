package com.sypztep;

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
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("leklai_server.json5"))
                    .setJson5(true)
                    .build())
            .build();

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

    @SerialEntry
    public float hotHydrationDrainMul = 1.0f;

    @SerialEntry
    public float coldEnergyDrainMul = 1.0f;
}