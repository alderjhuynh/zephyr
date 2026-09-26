package com.zephyr.client.mixin.qol.Zoom;

import com.zephyr.client.module.qol.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Camera} implementing the Zephyr Zoom module.
 *
 * <p>Injects at the return of {@code Camera.calculateFov} and replaces the
 * vanilla FOV with the module's smoothed zoom value whenever the module is
 * active (see {@link Zoom#apply}).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    /**
     * Applies the Zoom module's smoothed FOV on top of the vanilla calculation.
     *
     * @param partialTicks the current partial tick
     * @param cir          mixin callback used to substitute the FOV
     */
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void zephyr$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Zoom.INSTANCE.isActive()) {
            cir.setReturnValue(Zoom.INSTANCE.apply(cir.getReturnValue()));
        }
    }
}
