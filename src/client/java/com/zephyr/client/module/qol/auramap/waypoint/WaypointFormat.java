package com.zephyr.client.module.qol.auramap.waypoint;

public final class WaypointFormat {
    private WaypointFormat() {}

    public static String distance(double d, int precision, int kmThreshold) {
        if (kmThreshold >= 0 && d >= kmThreshold) {
            return fmt(d / 1000.0, precision) + "km";
        }
        return fmt(d, precision) + "m";
    }

    private static String fmt(double v, int precision) {
        if (precision <= 0) return String.valueOf((int) Math.round(v));
        String s = String.format("%." + Math.min(3, precision) + "f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    public static double fullDistance(Waypoint w, double px, double py, double pz) {
        double dx = (w.x() + 0.5) - px;
        double dy = w.yIncluded() ? (w.y() - py) : 0.0;
        double dz = (w.z() + 0.5) - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
