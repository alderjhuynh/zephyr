package com.zephyr.client.module.qol.auramap.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class WaypointTeleport {
    private WaypointTeleport() {}

    public static void teleportTo(Waypoint w) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int y = w.yIncluded() ? w.y() : (int) Math.floor(mc.player.getY());
        String cmd = w.rotation()
                ? "tp @s " + w.x() + " " + y + " " + w.z() + " " + w.yaw() + " ~"
                : "tp @s " + w.x() + " " + y + " " + w.z();
        try {
            var conn = mc.player.connection;
            if (conn != null) {
                conn.sendCommand(cmd);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            mc.player.connection.sendChat(cmd.startsWith("/") ? cmd : "/" + cmd);
        } catch (Throwable t) {
            if (mc.player != null) mc.player.sendSystemMessage(Component.literal("No permission to teleport."));
        }
    }
}
