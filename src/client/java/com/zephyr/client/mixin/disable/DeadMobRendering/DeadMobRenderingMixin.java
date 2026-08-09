package com.zephyr.client.mixin.disable.DeadMobRendering;

import com.zephyr.client.module.disable.disableDeadMobRendering;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class DeadMobRenderingMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void cancelDeadMobRendering(
            LivingEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!disableDeadMobRendering.INSTANCE.isEnabled()) return;
        if (state != null && state.deathTime > 0.0F) {
            ci.cancel();
        }
    }
}
