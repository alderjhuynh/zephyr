package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // Prevents attacks when allowInteract is disabled.
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (zephyr$disableInteract()) {
            cir.cancel();
        }
    }

    // Prevents item pick when allowInteract is disabled.
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockPick(CallbackInfo ci) {
        if (zephyr$disableInteract()) {
            ci.cancel();
        }
    }

    // Makes mouse clicks come from the player rather than the freecam entity when player control is enabled or if interaction mode is set to player.
    @ModifyVariable(method = "pick(F)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity zephyr$hitTargetSource(Entity entity) {
        if (FreeCam.INSTANCE.isEnabled() && (FreeCam.isPlayerControlEnabled() || FreeCam.allowInteractionsFromPlayer())) {
            return Minecraft.getInstance().player;
        }
        return entity;
    }

    // Prevents block breaking when allowInteract is disabled.
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockContinueAttack(CallbackInfo ci) {
        if (zephyr$disableInteract()) {
            ci.cancel();
        }
    }

    // Disables freecam if the player disconnects.
    @Inject(method = "disconnect*", at = @At(value = "HEAD"))
    private void zephyr$freecamDisconnect(CallbackInfo ci) {
        FreeCam.onDisconnect();
    }

    @Unique
    private static boolean zephyr$disableInteract() {
        return FreeCam.INSTANCE.isEnabled() && !FreeCam.isPlayerControlEnabled() && FreeCam.shouldPreventInteractions();
    }
}
