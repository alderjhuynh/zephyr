package com.zephyr.client.module;

import com.zephyr.client.mixin.PlayerMoveC2SPacketAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall {
    public static boolean enabled = true;
    private static final float MIN_FALL_DISTANCE = 2.5F;
    private static final double ELYTRA_LANDING_CHECK_DISTANCE = 0.6D;

    private NoFall() {
    }

    public static void onSendPacket(PlayerMoveC2SPacket packet) {
        if (!enabled) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (mc.player.getAbilities().flying) {
            return;
        }

        if (mc.player.isFallFlying()) {
            if (!isAboutToTouchGround(mc)) {
                return;
            }
        } else if (mc.player.fallDistance <= MIN_FALL_DISTANCE) {
            return;
        }

        ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
    }

    private static boolean isAboutToTouchGround(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            return false;
        }

        if (mc.player.getVelocity().y >= 0.0D) {
            return false;
        }

        return !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0D, -ELYTRA_LANDING_CHECK_DISTANCE, 0.0D));
    }
}
