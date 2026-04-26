package com.sypztep.temporature.config;

import com.sypztep.temporature.Temporature;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class TemporatureServerConfig {
    public TemporatureServerConfig() {}

    private static boolean syncedFromServer = false;

    public static TemporatureServerConfig getInstance() {
        return HANDLER.instance();
    }

    /** True when the client is using config values received from a server. */
    public static boolean isSyncedFromServer() { return syncedFromServer; }

    public static void setSyncedFromServer(boolean synced) { syncedFromServer = synced; }

    /**
     * Copies all @Sync fields from src into the live instance.
     * No manual field list needed — adding @Sync to a new field is enough.
     */
    public static void applyFrom(TemporatureServerConfig src) {
        ConfigSyncUtil.applyFrom(src, getInstance());
    }

    public static final ConfigClassHandler<TemporatureServerConfig> HANDLER = ConfigClassHandler
            .createBuilder(TemporatureServerConfig.class)
            .id(Temporature.id("config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("temporature_server.json5"))
                    .setJson5(true)
                    .build())
            .build();

    // --- Core Temperature ---

    @SerialEntry @RequireSync
    public boolean enableTemperatureSystem = true;

    @SerialEntry @RequireSync
    public double minHabitableTemp = 0.4;

    @SerialEntry @RequireSync
    public double maxHabitableTemp = 1.5;

    @SerialEntry @RequireSync
    public float tempRate = 1.0f;

    @SerialEntry @RequireSync
    public int tempDamageInterval = 40;

    @SerialEntry @RequireSync
    public float tempBaseDamage = 2f;

    @SerialEntry @RequireSync
    public int blockScanRadius = 7;

    // --- Wetness ---

    @SerialEntry @RequireSync
    public float waterSoakSpeed = 0.02f;

    @SerialEntry @RequireSync
    public float rainSoakSpeed = 0.005f;

    @SerialEntry @RequireSync
    public float maxRainWetness = 0.6f;

    @SerialEntry @RequireSync
    public float dryRate = 0.0008f;

    @SerialEntry @RequireSync
    public float hotDryBonus = 0.0008f;

    @SerialEntry @RequireSync
    public float coldDryMultiplier = 0.3f;

    @SerialEntry @RequireSync
    public double defaultWaterTemp = -0.93;

    @SerialEntry @RequireSync
    public float waterTempBiomeFactor = 0.7f;

    @SerialEntry @RequireSync
    public float waterTempOffset = 0.0f;

    @SerialEntry @RequireSync
    public float residualWaterDriftRate = 0.0004f;

    @SerialEntry @RequireSync
    public float rainWaterTempFactor = 0.8f;

    @SerialEntry @RequireSync
    public int maxWaterDepth = 30;

    @SerialEntry @RequireSync
    public double deepWaterTemp = 0.16;

    // --- Adaptation ---

    @SerialEntry @RequireSync
    public boolean enableAdaptation = true;

    @SerialEntry @RequireSync
    public float adaptRate = 0.00017f;

    @SerialEntry @RequireSync
    public float maxAdaptShift = 0.4f;

    @SerialEntry @RequireSync
    public float adaptStrength = 0.4f;

    @SerialEntry @RequireSync
    public float extremeThreshold = 0.5f;

    @Override
    public int hashCode() {
        return ConfigSyncUtil.syncHashCode(this);
    }
}