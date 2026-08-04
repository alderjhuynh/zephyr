package com.zephyr.client.mixin.disable.FogRendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.zephyr.client.module.disable.disableFogRendering;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {

    @Shadow @Final private GpuBuffer emptyBuffer;

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableFog(FogRenderer.FogMode mode, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (disableFogRendering.INSTANCE.isEnabled()) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
        }
    }
}