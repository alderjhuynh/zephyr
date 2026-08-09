package com.zephyr.client.mixin.qol.Zoom;

import com.zephyr.client.module.qol.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class CameraMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void zephyr$zoom(Camera camera, float partialTicks, boolean useFovSetting,
            CallbackInfoReturnable<Float> cir) {
        if (Zoom.INSTANCE.isActive()) {
            cir.setReturnValue(Zoom.INSTANCE.apply(cir.getReturnValue()));
        }
    }
}
