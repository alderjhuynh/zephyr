package com.zephyr.client.mixin.combat.HitAssist;

import com.zephyr.client.module.combat.HitAssist;
import com.zephyr.client.module.combat.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"),
            cancellable = true
    )
    private void zephyr$assistMissedAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!HitAssist.INSTANCE.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.level == null) return;

        Entity target = findAssistTarget(client);
        if (target == null) return;

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        cir.setReturnValue(true);
    }

    private static Entity findAssistTarget(Minecraft client) {
        LocalPlayer player = client.player;
        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            reach += Reach.INSTANCE.entityReach.get();
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxAngle = Math.toRadians(HitAssist.INSTANCE.angle.get());

        Entity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive()) continue;
            if (living.isSpectator()) continue;

            AABB box = living.getBoundingBox();
            Vec3 closest = new Vec3(
                    Mth.clamp(eye.x, box.minX, box.maxX),
                    Mth.clamp(eye.y, box.minY, box.maxY),
                    Mth.clamp(eye.z, box.minZ, box.maxZ)
            );
            Vec3 toTarget = closest.subtract(eye);
            double distance = toTarget.length();
            if (distance > reach || distance < 1.0E-4) continue;

            double angle = Math.acos(Mth.clamp(look.dot(toTarget.scale(1.0 / distance)), -1.0, 1.0));
            if (angle <= maxAngle && angle < bestAngle) {
                bestAngle = angle;
                best = living;
            }
        }
        return best;
    }
}
