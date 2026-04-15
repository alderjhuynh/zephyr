package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableRainEffects;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class NoRainRenderMixin {

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void disableWeatherRendering(CallbackInfo ci) {
        if (!disableRainEffects.enabled) {return;}
        ci.cancel();
    }
}