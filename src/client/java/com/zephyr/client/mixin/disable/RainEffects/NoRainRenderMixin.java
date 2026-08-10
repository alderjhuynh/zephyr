package com.zephyr.client.mixin.disable.RainEffects;

import com.zephyr.client.module.disable.disableRainEffects;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class NoRainRenderMixin {

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void disableWeatherRendering(CallbackInfo ci) {
        if (!disableRainEffects.INSTANCE.isEnabled()) {return;}
        ci.cancel();
    }
}
