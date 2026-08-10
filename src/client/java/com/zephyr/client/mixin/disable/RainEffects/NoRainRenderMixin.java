package com.zephyr.client.mixin.disable.RainEffects;

import com.zephyr.client.module.disable.disableRainEffects;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link WeatherEffectRenderer} that backs the
 * {@code disableRainEffects} module. It handles the rendering half of the module's
 * behaviour.
 */
@Mixin(WeatherEffectRenderer.class)
public class NoRainRenderMixin {

    /**
     * Cancels {@code WeatherEffectRenderer#render} at its head so that no rain or
     * weather particle rendering is drawn while the module is enabled.
     *
     * @param ci the cancellable injection callback
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void disableWeatherRendering(CallbackInfo ci) {
        if (!disableRainEffects.INSTANCE.isEnabled()) {return;}
        ci.cancel();
    }
}
