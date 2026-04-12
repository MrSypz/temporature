package com.sypztep.system.temperature;

public final class LayerOrder {
    final WorldTemperatureLayer anchor;
    final boolean after;

    private LayerOrder(WorldTemperatureLayer anchor, boolean after) {
        this.anchor = anchor;
        this.after = after;
    }

    public static LayerOrder after(WorldTemperatureLayer anchor) {
        return new LayerOrder(anchor, true);
    }

    public static LayerOrder before(WorldTemperatureLayer anchor) {
        return new LayerOrder(anchor, false);
    }
}
