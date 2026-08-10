package com.zephyr.client.mixin.disable.FogRendering;

import com.zephyr.client.module.disable.disableFogRendering;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void zephyr$disableFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        if (disableFogRendering.INSTANCE.isEnabled()) {
            FogRenderer.setupNoFog();
            ci.cancel();
        }
    }
}
