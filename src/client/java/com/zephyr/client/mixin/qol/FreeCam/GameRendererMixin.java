package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Hide hand in freecam if showHand is disabled.
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideHand(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldHideHand()) {
            ci.cancel();
        }
    }

    // Disables block outlines when allowInteract is disabled.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && !FreeCam.isPlayerControlEnabled() && FreeCam.shouldPreventInteractions()) {
            cir.setReturnValue(false);
        }
    }
}
