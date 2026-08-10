package com.zephyr.client.mixin.qol.FreeCam;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin into {@link Lightmap} that forces the lightmap to its maximum
 * brightness while the Zephyr FreeCam module's "Full Bright" setting is on.
 *
 * <p>Wraps the read of {@link LightmapRenderState#brightness} during
 * {@code Lightmap.render} and substitutes the max light value (16.0f) instead,
 * effectively giving the detached camera full-bright vision without the Full
 * Bright module.
 */
@Mixin(Lightmap.class)
public abstract class FullBrightMixin {
    /**
     * Returns the maximum brightness when FreeCam's "Full Bright" is enabled,
     * otherwise defers to the original lightmap value.
     *
     * @param instance the lightmap render state whose brightness is read
     * @param original the wrapped field-read operation
     * @return the brightness value to use
     */
    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;brightness:F"))
    private float zephyr$freecamFullbright(LightmapRenderState instance, Operation<Float> original) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.isFullBrightEnabled()) {
            return 16.0f;
        }
        return original.call(instance);
    }
}
