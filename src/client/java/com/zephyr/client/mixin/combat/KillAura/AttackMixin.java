package com.zephyr.client.mixin.combat.KillAura;

import com.zephyr.client.module.combat.KillAura.TargetManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {

    @Inject(
        method = "attack",
        at = @At("HEAD")
    )

    private void zephyr$storeTarget(Player player, Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            TargetManager.setTarget(living);
        } else {
            TargetManager.clear();
        }
    }
}