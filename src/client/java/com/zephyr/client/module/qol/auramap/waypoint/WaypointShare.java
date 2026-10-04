package com.zephyr.client.module.qol.auramap.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class WaypointShare {
    private WaypointShare() {}

    public static String encode(Waypoint w) {
        return "[auramap-waypoint name=\"" + w.name().replace("\"", "'") + "\" x=" + w.x()
                + " y=" + w.y() + " z=" + w.z() + " color=" + w.color().name() + "]";
    }

    public static void share(Waypoint w) {
        var mc = Minecraft.getInstance();
        String s = encode(w);
        try { mc.keyboardHandler.setClipboard(s); } catch (Throwable ignored) {}
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("Waypoint copied: " + s));
        }
    }

    public static Waypoint decode(String text) {
        if (text == null) return null;
        text = text.trim();
        try {
            if (text.startsWith("[auramap-waypoint")) {
                String name = "Waypoint";
                int x = 0, y = 64, z = 0;
                WaypointColor color = WaypointColor.YELLOW;
                int ni = text.indexOf("name=\"");
                if (ni >= 0) {
                    int end = text.indexOf('"', ni + 6);
                    if (end > ni) name = text.substring(ni + 6, end);
                }
                x = parseIntAfter(text, "x=");
                y = parseIntAfter(text, "y=");
                z = parseIntAfter(text, "z=");
                int ci = text.indexOf("color=");
                if (ci >= 0) {
                    String c = text.substring(ci + 6).replaceAll("[^A-Za-z_]", "");
                    color = WaypointColor.fromName(c, WaypointColor.YELLOW);
                }
                return new Waypoint(name, name.length() > 2 ? name.substring(0, 2) : name, x, y, z, color);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int parseIntAfter(String text, String key) {
        int i = text.indexOf(key);
        if (i < 0) return 0;
        int j = i + key.length();
        int k = j;
        while (k < text.length() && (Character.isDigit(text.charAt(k)) || text.charAt(k) == '-')) k++;
        try { return Integer.parseInt(text.substring(j, k)); } catch (Exception e) { return 0; }
    }
}
