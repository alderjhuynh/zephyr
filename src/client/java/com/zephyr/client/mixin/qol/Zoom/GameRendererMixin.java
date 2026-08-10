package com.zephyr.client.mixin.qol.Zoom;

import com.zephyr.client.module.qol.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void zephyr$zoom(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (Zoom.INSTANCE.isActive()) {
            cir.setReturnValue((double) Zoom.INSTANCE.apply(cir.getReturnValue().floatValue()));
        }
    }
}
