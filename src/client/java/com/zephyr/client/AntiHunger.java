package com.zephyr.client;

import com.zephyr.client.mixin.PlayerMoveC2SPacketAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiHunger {

    public static boolean enabled = true;

    private static boolean lastOnGround = false;
    private static boolean ignoreNextMovePacket = false;

    public static void onTick(MinecraftClient mc) {
        if (!enabled) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        boolean onGround = player.isOnGround();

        if (onGround && !lastOnGround) {
            ignoreNextMovePacket = true;
        }

        lastOnGround = onGround;
    }

    public static boolean onSendPacket(Object packet, MinecraftClient mc) {
        if (!enabled || mc.player == null) return true;

        ClientPlayerEntity player = mc.player;

        if (player.hasVehicle()
                || player.isTouchingWater()
                || player.isSubmergedInWater()) {
            return true;
        }

        if (packet instanceof ClientCommandC2SPacket cmd) {
            return cmd.getMode() != ClientCommandC2SPacket.Mode.START_SPRINTING;
        }

        if (packet instanceof PlayerMoveC2SPacket move) {
            if (ignoreNextMovePacket) {
                ignoreNextMovePacket = false;
                return true;
            }

            if (player.isOnGround()
                    && player.fallDistance <= 0.0f
                    && !mc.interactionManager.isBreakingBlock()) {

                ((PlayerMoveC2SPacketAccessor) move).setOnGround(false);
            }
        }

        return true;
    }
}