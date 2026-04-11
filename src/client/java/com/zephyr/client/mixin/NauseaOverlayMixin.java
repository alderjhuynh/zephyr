package com.zephyr.client.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gui.DrawContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableNauseaOverlay;

@Mixin(GameRenderer.class)
public class NauseaOverlayMixin {

    @Inject(
            method = "renderNausea",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableNauseaEffect(DrawContext context, float distortionStrength, CallbackInfo ci) {
        if (disableNauseaOverlay.enabled) {
            ci.cancel();
        }
    }
}
