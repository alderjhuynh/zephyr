package com.zephyr.client.mixin.disable.FogRendering;

import com.zephyr.client.module.disable.disableFogRendering;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;
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

    @Inject(method = "setupFog", at = @At("HEAD"))
    private void zephyr$disableFog(
            Camera camera,
            int viewDistance,
            DeltaTracker tickCounter,
            float skyDarkness,
            ClientLevel world,
            CallbackInfoReturnable<FogData> cir
    ) {
        zephyr$previousFogEnabled = fogEnabled;
        if (disableFogRendering.INSTANCE.isEnabled()) {
            fogEnabled = false;
        }
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void zephyr$restoreFog(
            Camera camera,
            int viewDistance,
            DeltaTracker tickCounter,
            float skyDarkness,
            ClientLevel world,
            CallbackInfoReturnable<FogData> cir
    ) {
        fogEnabled = zephyr$previousFogEnabled;
    }
}
