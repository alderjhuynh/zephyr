package com.zephyr.client.module.qol.auramap.waypoint;

public enum WaypointSort {
    NONE("Unsorted"),
    NAME("Name"),
    COLOR("Color"),
    DISTANCE("Distance"),
    ANGLE("Angle");

    private final String label;
    WaypointSort(String label) { this.label = label; }
    public String label() { return label; }
}
