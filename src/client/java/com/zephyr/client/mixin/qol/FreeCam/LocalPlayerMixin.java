package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    // Needed for Baritone compatibility.
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void zephyr$isControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player) {
            cir.setReturnValue(true);
        }
    }

    // Makes rotation depend upon FreeCamera rather than the player.
    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void zephyr$getViewXRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (FreeCam.isActive() && !FreeCam.isPlayerControlEnabled() && !FreeCam.allowInteractionsFromPlayer()) {
            cir.setReturnValue(FreeCam.getFreeCamera().getViewXRot(partialTick));
        }
    }

    // Makes rotation depend upon FreeCamera rather than the player.
    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void zephyr$getViewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (FreeCam.isActive() && !FreeCam.isPlayerControlEnabled() && !FreeCam.allowInteractionsFromPlayer()) {
            cir.setReturnValue(FreeCam.getFreeCamera().getViewYRot(partialTick));
        }
    }

    @Unique
    private LocalPlayer freecam$this() {
        return (LocalPlayer) (Object) this;
    }
}
