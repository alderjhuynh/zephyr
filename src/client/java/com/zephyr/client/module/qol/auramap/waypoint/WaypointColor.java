package com.zephyr.client.module.qol.auramap.waypoint;

public enum WaypointColor {
    BLACK("Black", '0', 0xFF000000),
    DARK_BLUE("Dark Blue", '1', 0xFF0000AA),
    DARK_GREEN("Dark Green", '2', 0xFF00AA00),
    DARK_AQUA("Dark Aqua", '3', 0xFF00AAAA),
    DARK_RED("Dark Red", '4', 0xFFAA0000),
    DARK_PURPLE("Dark Purple", '5', 0xFFAA00AA),
    GOLD("Gold", '6', 0xFFFFAA00),
    GRAY("Gray", '7', 0xFFAAAAAA),
    DARK_GRAY("Dark Gray", '8', 0xFF555555),
    BLUE("Blue", '9', 0xFF5555FF),
    GREEN("Green", 'a', 0xFF55FF55),
    AQUA("Aqua", 'b', 0xFF55FFFF),
    RED("Red", 'c', 0xFFFF5555),
    PURPLE("Light Purple", 'd', 0xFFFF55FF),
    YELLOW("Yellow", 'e', 0xFFFFFF55),
    WHITE("White", 'f', 0xFFFFFFFF),
    MAGENTA("Magenta", 'f', 0xFFFF00FF | 0xFF000000),
    LIGHT_BLUE("Light Blue", 'f', 0xFF86B9FF),
    LIME("Lime", 'f', 0xFF7CFC00),
    PINK("Pink", 'f', 0xFFFFAEC9),
    BROWN("Brown", 'f', 0xFF8B5A2B);

    private final String label;
    private final char code;
    private final int argb;

    WaypointColor(String label, char code, int argb) {
        this.label = label;
        this.code = code;
        this.argb = argb;
    }

    public String label() { return label; }
    public char code() { return code; }
    public int argb() { return argb; }

    public static WaypointColor fromIndex(int i) {
        var v = values();
        if (i < 0) i = 0;
        if (i >= v.length) i = v.length - 1;
        return v[i];
    }

    public static WaypointColor fromName(String n, WaypointColor fallback) {
        if (n == null) return fallback;
        try { return valueOf(n.toUpperCase()); } catch (IllegalArgumentException e) { return fallback; }
    }

    public static WaypointColor random() {
        var v = values();
        return v[(int) (Math.random() * v.length)];
    }

    public int rgb() { return argb & 0xFFFFFF; }
}
