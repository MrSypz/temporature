package com.sypztep.config;

import com.sypztep.Temporature;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Objects;

public final class TemporatureServerConfig {
    public TemporatureServerConfig() {}

    private static boolean syncedFromServer = false;

    public static TemporatureServerConfig getInstance() {
        return HANDLER.instance();
    }

    /** True when the client is using config values received from a server. */
    public static boolean isSyncedFromServer() { return syncedFromServer; }

    public static void setSyncedFromServer(boolean synced) { syncedFromServer = synced; }

    /** Copy all fields from a deserialized config into the live instance. */
    public static void applyFrom(TemporatureServerConfig src) {
        TemporatureServerConfig dst = getInstance();
        dst.enableTemperatureSystem = src.enableTemperatureSystem;
        dst.minHabitableTemp = src.minHabitableTemp;
        dst.maxHabitableTemp = src.maxHabitableTemp;
        dst.tempRate = src.tempRate;
        dst.tempDamageInterval = src.tempDamageInterval;
        dst.tempBaseDamage = src.tempBaseDamage;
        dst.blockScanRadius = src.blockScanRadius;
        dst.waterSoakSpeed = src.waterSoakSpeed;
        dst.rainSoakSpeed = src.rainSoakSpeed;
        dst.maxRainWetness = src.maxRainWetness;
        dst.dryRate = src.dryRate;
        dst.hotDryBonus = src.hotDryBonus;
        dst.coldDryMultiplier = src.coldDryMultiplier;
        dst.defaultWaterTemp = src.defaultWaterTemp;
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

    @Override
    public int hashCode() {
        return Objects.hash(
                enableTemperatureSystem,
                minHabitableTemp, maxHabitableTemp,
                tempRate, tempDamageInterval, tempBaseDamage, blockScanRadius,
                waterSoakSpeed, rainSoakSpeed, maxRainWetness,
                dryRate, hotDryBonus, coldDryMultiplier, defaultWaterTemp
        );
    }
}
