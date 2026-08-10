package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // Makes the local player render with a glowing team-color outline while freecam is active.
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.isOutlineEnabled() && entity == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }

    // Prevents attacks when allowInteract is disabled.
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (zephyr$disableInteract()) {
            cir.cancel();
        }
    }

    // Prevents item pick when allowInteract is disabled.
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockPick(CallbackInfo ci) {
        if (zephyr$disableInteract()) {
            ci.cancel();
        }
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
