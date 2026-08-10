package com.zephyr.client.mixin.disable.FluidFog;

import com.zephyr.client.module.disable.disableFluidFog;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void zephyr$removeFluidFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        if (!disableFluidFog.INSTANCE.isEnabled()) return;
        FogType type = camera.getFluidInCamera();
        if (type == FogType.WATER || type == FogType.LAVA) {
            FogRenderer.setupNoFog();
            ci.cancel();
        }
    }
}
