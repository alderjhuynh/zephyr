package com.zephyr.client.module.qol.auramap.waypoint;

public final class WaypointIcon {
    private WaypointIcon() {}

    public static int frameFor(int initialsWidthPx) {
        if (initialsWidthPx <= 8) return 0;
        int total = initialsWidthPx - 8;
        return total - total / 2;
    }

    public static boolean isDeath(Waypoint w) {
        return w != null && w.isDeath();
    }

    public static String deathGlyph() {
        return "\u2620";
    }
}
