package com.zephyr.client.mixin.qol.Zoom;

import com.zephyr.client.module.qol.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void zephyr$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Zoom.INSTANCE.isActive()) {
            cir.setReturnValue(Zoom.INSTANCE.apply(cir.getReturnValue()));
        }
    }
}
