package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class ShieldBreaker {
    public static boolean enabled = true;
    public static boolean isLegit = true;

    public static boolean isTargetBlockingWithShield(PlayerEntity target, PlayerEntity attacker) {
        if (target == null || attacker == null) return false;

        if (!target.isUsingItem()) return false;

        if (!target.getActiveItem().isOf(Items.SHIELD)) return false;

        Vec3d targetLook = target.getRotationVec(1.0f).normalize();
        Vec3d directionToAttacker = attacker.getEntityPos().subtract(target.getEntityPos()).normalize();

        double dot = targetLook.dotProduct(directionToAttacker);

        // threshold
        return dot > 0.3;
    }

    public static boolean isCrosshairOnPlayer(PlayerEntity localPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.crosshairTarget == null) return false;

        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hitResult)) {
            return false;
        }

        return hitResult.getEntity() instanceof PlayerEntity;
    }

    public static PlayerEntity getCrosshairPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hitResult) {
            if (hitResult.getEntity() instanceof PlayerEntity player) {
                return player;
            }
        }

        return null;
    }
}