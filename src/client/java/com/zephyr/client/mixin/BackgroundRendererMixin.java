package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableFogRendering;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {
    @Shadow private static boolean fogEnabled;
    @Unique private static boolean zephyr$previousFogEnabled;

    @Inject(method = "applyFog", at = @At("HEAD"))
    private void zephyr$disableFog(
            Camera camera,
            int viewDistance,
            RenderTickCounter tickCounter,
            float skyDarkness,
            ClientWorld world,
            CallbackInfoReturnable<Vector4f> cir
    ) {
        zephyr$previousFogEnabled = fogEnabled;
        if (disableFogRendering.enabled) {
            fogEnabled = false;
        }
    }

    @Inject(method = "applyFog", at = @At("RETURN"))
    private void zephyr$restoreFog(
            Camera camera,
            int viewDistance,
            RenderTickCounter tickCounter,
            float skyDarkness,
            ClientWorld world,
            CallbackInfoReturnable<Vector4f> cir
    ) {
        fogEnabled = zephyr$previousFogEnabled;
    }
}
