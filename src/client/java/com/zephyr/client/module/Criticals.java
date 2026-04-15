package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals {
    public static boolean enabled = false;
    private static double spoofHeight = 2;

    private Criticals() {}

    public static double getSpoofHeight() {
        return spoofHeight;
    }

    public static void setSpoofHeight(double spoofHeight) {
        Criticals.spoofHeight = Math.max(0.0, spoofHeight);
        ZephyrConfig.saveCurrentState();
    }

    public static void onAttack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null) return;

        if (!mc.player.isOnGround()) return;
        if (mc.player.hasVehicle() || mc.player.isClimbing()) return;
        if (mc.player.isInLava() || mc.player.isTouchingWater()) return;

        spoofCritPackets(mc);
    }

    public static void forceCrit() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null) return;

        spoofCritPackets(mc);
    }

    private static void spoofCritPackets(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        sendPos(x, y + spoofHeight, z, false);
        sendPos(x, y, z, false);
    }

    private static void sendPos(double x, double y, double z, boolean onGround) {
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false)
        );
    }
}
