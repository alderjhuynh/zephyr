package com.zephyr.client.mixin.disable.DisableDamageTilt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableDamageTilt;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's GameRenderer.bobHurt cancel. 1.21.1 signature is
// bobHurt(PoseStack, float); 26.3 uses (CameraRenderState, PoseStack).
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableDamageTilt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (disableDamageTilt.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
