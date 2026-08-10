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

/**
 * Mixin targeting {@link FogRenderer} that backs the {@code disableFogRendering}
 * module.
 */
@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {

    @Shadow @Final private GpuBuffer emptyBuffer;

    /**
     * Intercepts {@code FogRenderer#getBuffer} at its head: when the module is enabled
     * the returned fog uniform buffer slice is replaced with an empty one, so no fog is
     * applied to the frame.
     *
     * @param mode the fog mode being requested
     * @param cir  the cancellable return-value callback
     */
    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableFog(FogRenderer.FogMode mode, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (disableFogRendering.INSTANCE.isEnabled()) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
        }
    }
}