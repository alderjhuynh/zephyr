package com.zephyr.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import com.zephyr.client.module.F5Tweaks;
import com.zephyr.client.module.FreeCam;

@Mixin(Camera.class)
public class CameraClipMixin {

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void f5mod$noClip(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        if (FreeCam.isActive() || F5Tweaks.shouldFreeLook(MinecraftClient.getInstance().options.getPerspective())) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }
}
