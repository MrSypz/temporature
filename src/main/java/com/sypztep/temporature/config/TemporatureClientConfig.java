package com.sypztep.temporature.config;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.client.TemperatureUnit;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class TemporatureClientConfig {
    public TemporatureClientConfig() {}
    public static TemporatureClientConfig getInstance() {
        return HANDLER.instance();
    }
    public static final ConfigClassHandler<TemporatureClientConfig> HANDLER = ConfigClassHandler.createBuilder(TemporatureClientConfig.class)
            .id(Temporature.id("client_config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("temporature_client.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public boolean showWorldGauge = true;

    @SerialEntry
    public boolean worldGaugeMeterSound = true;

    @SerialEntry
    public TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;
}