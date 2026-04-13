package com.sypztep.system.temperature;

import com.sypztep.TemporatureServerConfig;
import net.minecraft.world.entity.player.Player;

/**
 * Core temperature math. All internal values are in <b>MC units</b>
 * anchor scale:
 * <ul>
 *   <li>1 MC = 25°C = 45°F (relative)</li>
 *   <li>Absolute 0 MC = 0°C = 32°F</li>
 * </ul>
 * <p>
 * There are two independent temperature scales:
 * <ol>
 *   <li><b>worldTemp</b> — ambient temperature in MC (unclamped, can exceed habitable band arbitrarily)</li>
 *   <li><b>coreTemp</b> — body heat accumulator in an abstract ±150 scale, with damage at ±100.
 *       NOT the same unit as worldTemp. Grows when worldTemp is outside the habitable band,
 *       drifts toward 0 when inside.</li>
 * </ol>
 */
public final class TemperatureHelper {
    // --- Unit conversions (MC ↔ Celsius ↔ Fahrenheit) ---
    public static double mcToC(double mc) {
        return mc * 25.0;
    }

    public static double cToMc(double c) {
        return c / 25.0;
    }

    public static double mcToF(double mc) {
        return mc * 45.0 + 32.0;
    }

    public static double fToMc(double f) {
        return (f - 32.0) / 45.0;
    }

    public static double mcToCDelta(double mc) {
        return mc * 25.0;
    }

    public static double cToMcDelta(double c) {
        return c / 25.0;
    }

    // --- Core accumulator bounds ---
    public static final double CORE_MIN = -150.0;
    public static final double CORE_MAX = 150.0;

    // --- Core zone thresholds (on the core accumulator scale) ---
    public static final double WARM_DEV = 25.0;
    public static final double CHILLY_DEV = -25.0;
    public static final double HEATSTROKE_DEV = 50.0;
    public static final double HYPOTHERMIA_DEV = -50.0;
    public static final double BURNING_DEV = 100.0;
    public static final double FREEZING_DEV = -100.0;

    // --- Neutral body ---
    public static final double NEUTRAL_BODY_C = 37.0;

    private TemperatureHelper() {
    }

    public static double calculateWorldTemp(Player player) {
        return TemperatureLayerRegistry.execute(player);
    }

    public static double clampCore(double core) {
        return Math.clamp(core, CORE_MIN, CORE_MAX);
    }

    /**
     * Maps the core accumulator back to a display Celsius value.
     * <p>
     * core = 0   → 37°C (neutral)
     * core = ±100 → ±5°C from neutral (32°C / 42°C) — matches real-world damage thresholds
     * core = ±150 → ±7.5°C from neutral
     */
    public static double coreToCelsius(double core) {
        return NEUTRAL_BODY_C + (core / 100.0) * 5.0;
    }

    /**
     * Safe habitable band [min, max] in MC units, from config.
     */
    public static double minHabitable() {
        return TemporatureServerConfig.getInstance().minHabitableTemp;
    }

    public static double maxHabitable() {
        return TemporatureServerConfig.getInstance().maxHabitableTemp;
    }

    /**
     * Returns the sign of how worldTemp sits against the safe band:
     * -1 if below min (cold), +1 if above max (hot), 0 if inside (safe).
     */
    public static int worldTempSign(double worldTemp) {
        double min = minHabitable();
        double max = maxHabitable();
        if (worldTemp < min) return -1;
        if (worldTemp > max) return 1;
        return 0;
    }

    /**
     * Distance from worldTemp to the nearest safe-band edge (always ≥ 0).
     * Returns 0 if worldTemp is inside the safe band.
     */
    public static double safeBandDifference(double worldTemp) {
        double min = minHabitable();
        double max = maxHabitable();
        return Math.abs(worldTemp - Math.clamp(worldTemp, min, max));
    }

    public static TempZone zoneFor(double core) {
        if (core <= FREEZING_DEV) return TempZone.FREEZING;
        if (core <= HYPOTHERMIA_DEV) return TempZone.HYPOTHERMIA;
        if (core <= CHILLY_DEV) return TempZone.CHILLY;
        if (core >= BURNING_DEV) return TempZone.BURNING;
        if (core >= HEATSTROKE_DEV) return TempZone.HEATSTROKE;
        if (core >= WARM_DEV) return TempZone.WARM;
        return TempZone.NORMAL;
    }

    public enum TempZone {
        FREEZING, HYPOTHERMIA, CHILLY, NORMAL, WARM, HEATSTROKE, BURNING
    }
}
