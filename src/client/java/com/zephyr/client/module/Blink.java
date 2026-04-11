package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Blink {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean CanUseKeybind = false;
    public static boolean ENABLED = false;

    private static final boolean RENDER_ORIGINAL = true;
    private static final int DELAY_TICKS = 0;

    private static final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private static OtherClientPlayerEntity fakePlayer;

    private static Vec3d startPos = Vec3d.ZERO;

    private static boolean sending = false;
    private static boolean cancelled = false;
    private static int timer = 0;
    private static ItemStack[] inventorySnapshot = new ItemStack[0];
    private static int selectedSlot = -1;


    public static void enable() {
        if (ENABLED || mc.player == null || mc.world == null) return;

        ENABLED = true;
        cancelled = false;
        timer = 0;
        packets.clear();

        startPos = mc.player.getPos();

        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }

        if (RENDER_ORIGINAL) {
            fakePlayer = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
            copyPlayerState(mc.player, fakePlayer);

            mc.world.addEntity(fakePlayer);
            syncFakeInventory(mc.player);
        }
    }

    public static void disable() {
        if (!ENABLED && packets.isEmpty() && fakePlayer == null) {
            cancelled = false;
            timer = 0;
            return;
        }

        ENABLED = false;

        dumpPackets(!cancelled);

        if (cancelled && mc.player != null) {
            mc.player.updatePosition(startPos.x, startPos.y, startPos.z);
            mc.player.setVelocity(Vec3d.ZERO);
        }

        cancelled = false;
    }

    public static void toggle() {
        if (ENABLED) {
            disable();
            return;
        }

        enable();
    }

    public static void tick(MinecraftClient client) {
        if (!ENABLED || client.player == null) return;

        if (fakePlayer != null) {
            double maxDistanceSq = 7.0 * 7.0;

            if (client.player.squaredDistanceTo(fakePlayer) > maxDistanceSq) {
                disable();
                return;
            }
        }

        timer++;

        if (fakePlayer != null && hasInventoryChanged(client.player.getInventory())) {
            syncFakeInventory(client.player);
        }

        if (DELAY_TICKS > 0 && timer >= DELAY_TICKS) {
            disable();
            enable();
        }
    }

    public static boolean onSendPacket(Object packet) {
        if (!ENABLED || sending) return false;

        if (!(packet instanceof PlayerMoveC2SPacket p)) return false;

        PlayerMoveC2SPacket prev = packets.isEmpty() ? null : packets.get(packets.size() - 1);

        if (prev != null &&
                p.isOnGround() == prev.isOnGround() &&
                p.getYaw(-1) == prev.getYaw(-1) &&
                p.getPitch(-1) == prev.getPitch(-1) &&
                p.getX(-1) == prev.getX(-1) &&
                p.getY(-1) == prev.getY(-1) &&
                p.getZ(-1) == prev.getZ(-1)
        ) {
            return true;
        }

        packets.add(p);
        return true;
    }

    public static void cancel() {
        cancelled = true;
        disable();
    }

    private static void dumpPackets(boolean send) {
        sending = true;

        if (send && mc.getNetworkHandler() != null) {
            for (PlayerMoveC2SPacket packet : packets) {
                mc.getNetworkHandler().sendPacket(packet);
            }
        }

        packets.clear();
        sending = false;

        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }

        inventorySnapshot = new ItemStack[0];
        selectedSlot = -1;
        timer = 0;
    }

    public static String getInfo() {
        return String.format("%.1f", timer / 20f);
    }

    private static void copyPlayerState(ClientPlayerEntity realPlayer, OtherClientPlayerEntity mirrorPlayer) {
        mirrorPlayer.refreshPositionAndAngles(
                realPlayer.getX(),
                realPlayer.getY(),
                realPlayer.getZ(),
                realPlayer.getYaw(),
                realPlayer.getPitch()
        );
        mirrorPlayer.prevYaw = realPlayer.prevYaw;
        mirrorPlayer.prevPitch = realPlayer.prevPitch;
        mirrorPlayer.setHeadYaw(realPlayer.getHeadYaw());
        mirrorPlayer.setBodyYaw(realPlayer.getBodyYaw());
        mirrorPlayer.setPose(realPlayer.getPose());
        mirrorPlayer.setOnGround(realPlayer.isOnGround());
    }

    private static boolean hasInventoryChanged(PlayerInventory inventory) {
        if (selectedSlot != inventory.selectedSlot || inventorySnapshot.length != inventory.size()) {
            return true;
        }

        for (int i = 0; i < inventory.size(); i++) {
            if (!ItemStack.areEqual(inventorySnapshot[i], inventory.getStack(i))) {
                return true;
            }
        }

        return false;
    }

    private static void syncFakeInventory(ClientPlayerEntity realPlayer) {
        if (fakePlayer == null) {
            return;
        }

        fakePlayer.getInventory().clone(realPlayer.getInventory());
        selectedSlot = realPlayer.getInventory().selectedSlot;
        inventorySnapshot = new ItemStack[realPlayer.getInventory().size()];

        for (int i = 0; i < realPlayer.getInventory().size(); i++) {
            inventorySnapshot[i] = realPlayer.getInventory().getStack(i).copy();
        }
    }
}
