package com.zephyr.client.mixin.qol.FreeCam;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Lightmap.class)
public abstract class FullBrightMixin {
    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;brightness:F"))
    private float zephyr$freecamFullbright(LightmapRenderState instance, Operation<Float> original) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.isFullBrightEnabled()) {
            return 16.0f;
        }
        return original.call(instance);
    }
}
