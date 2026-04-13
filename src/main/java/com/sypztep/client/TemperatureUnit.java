package com.sypztep.client;

import com.sypztep.system.temperature.TemperatureHelper;

public enum TemperatureUnit {
    CELSIUS("C"),
    FAHRENHEIT("F");

    private final String suffix;

    TemperatureUnit(String suffix) {
        this.suffix = suffix;
    }

    public double fromMc(double mc) {
        return switch (this) {
            case CELSIUS -> TemperatureHelper.mcToC(mc);
            case FAHRENHEIT -> TemperatureHelper.mcToF(mc);
        };
    }

    public String format(double mc) {
        return String.format("%.1f°%s", fromMc(mc), suffix);
    }
}
