package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import com.zephyr.client.mixin.ClientWorldAccessor;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class FakePlayerMirror {
    private OtherClientPlayerEntity fakePlayer;
    private ItemStack[] inventorySnapshot = new ItemStack[0];
    private int selectedSlot = -1;

    public void spawn(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        discard();

        fakePlayer = new OtherClientPlayerEntity(client.world, client.player.getGameProfile());
        copyPlayerState(client.player, fakePlayer);

        client.world.addEntity(fakePlayer);
        addPlayerToClientWorld(client, fakePlayer);
        syncInventory(client.player);
    }

    public void syncIfNeeded(ClientPlayerEntity realPlayer) {
        if (fakePlayer != null && hasInventoryChanged(realPlayer.getInventory())) {
            syncInventory(realPlayer);
        }
    }

    public boolean isSpawned() {
        return fakePlayer != null;
    }

    public void discard() {
        if (fakePlayer != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                removePlayerFromClientWorld(client, fakePlayer);
                client.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            } else {
                fakePlayer.discard();
            }
            fakePlayer = null;
        }

        inventorySnapshot = new ItemStack[0];
        selectedSlot = -1;
    }

    private static void copyPlayerState(ClientPlayerEntity realPlayer, OtherClientPlayerEntity mirrorPlayer) {
        mirrorPlayer.copyPositionAndRotation(realPlayer);
        Vec3d position = new Vec3d(realPlayer.getX(), realPlayer.getY(), realPlayer.getZ());
        mirrorPlayer.refreshPositionAndAngles(position, realPlayer.getYaw(), realPlayer.getPitch());
        mirrorPlayer.updateTrackedPositionAndAngles(position, realPlayer.getYaw(), realPlayer.getPitch());
        mirrorPlayer.updateTrackedHeadRotation(realPlayer.getHeadYaw(), 3);
        mirrorPlayer.setVelocity(realPlayer.getVelocity());
        mirrorPlayer.lastYaw = realPlayer.lastYaw;
        mirrorPlayer.lastPitch = realPlayer.lastPitch;
        mirrorPlayer.lastX = realPlayer.lastX;
        mirrorPlayer.lastY = realPlayer.lastY;
        mirrorPlayer.lastZ = realPlayer.lastZ;
        mirrorPlayer.setHeadYaw(realPlayer.getHeadYaw());
        mirrorPlayer.setBodyYaw(realPlayer.getBodyYaw());
        mirrorPlayer.setPose(realPlayer.getPose());
        mirrorPlayer.setOnGround(realPlayer.isOnGround());
        mirrorPlayer.setSprinting(realPlayer.isSprinting());
        mirrorPlayer.setSneaking(realPlayer.isSneaking());
        mirrorPlayer.setSwimming(realPlayer.isSwimming());
        mirrorPlayer.setGlowing(realPlayer.isGlowing());
        if (realPlayer.isUsingItem()) {
            mirrorPlayer.setCurrentHand(realPlayer.getActiveHand());
        } else {
            mirrorPlayer.clearActiveItem();
        }

        copyPoseFlags(realPlayer, mirrorPlayer);
    }

    private boolean hasInventoryChanged(PlayerInventory inventory) {
        if (selectedSlot != inventory.getSelectedSlot() || inventorySnapshot.length != inventory.size()) {
            return true;
        }

        for (int i = 0; i < inventory.size(); i++) {
            if (!ItemStack.areEqual(inventorySnapshot[i], inventory.getStack(i))) {
                return true;
            }
        }

        return false;
    }

    private void syncInventory(ClientPlayerEntity realPlayer) {
        if (fakePlayer == null) {
            return;
        }

        fakePlayer.getInventory().clone(realPlayer.getInventory());
        selectedSlot = realPlayer.getInventory().getSelectedSlot();
        inventorySnapshot = new ItemStack[realPlayer.getInventory().size()];

        for (int i = 0; i < realPlayer.getInventory().size(); i++) {
            inventorySnapshot[i] = realPlayer.getInventory().getStack(i).copy();
        }
    }

    private static void addPlayerToClientWorld(MinecraftClient client, OtherClientPlayerEntity player) {
        ClientWorldAccessor accessor = (ClientWorldAccessor) client.world;
        if (!accessor.zephyr$getPlayers().contains(player)) {
            accessor.zephyr$getPlayers().add(player);
        }
    }

    private static void removePlayerFromClientWorld(MinecraftClient client, OtherClientPlayerEntity player) {
        ClientWorldAccessor accessor = (ClientWorldAccessor) client.world;
        accessor.zephyr$getPlayers().remove(player);
    }

    private static void copyPoseFlags(ClientPlayerEntity realPlayer, OtherClientPlayerEntity mirrorPlayer) {
        if (realPlayer.isGliding()) {
            mirrorPlayer.setPose(EntityPose.GLIDING);
            return;
        }

        if (realPlayer.isSleeping()) {
            mirrorPlayer.setPose(EntityPose.SLEEPING);
            return;
        }

        if (realPlayer.isSwimming()) {
            mirrorPlayer.setPose(EntityPose.SWIMMING);
            return;
        }

        if (realPlayer.isSneaking()) {
            mirrorPlayer.setPose(EntityPose.CROUCHING);
            return;
        }

        mirrorPlayer.setPose(EntityPose.STANDING);
    }
}
