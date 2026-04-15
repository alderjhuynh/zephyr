package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableDeadMobRendering;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class DeadMobRenderingMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelDeadMobRendering(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!disableDeadMobRendering.enabled) return;
        if (state != null && state.deathTime > 0.0F) {
            ci.cancel();
        }
    }
}
