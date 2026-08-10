package com.zephyr.client.mixin.disable.RainEffects;

import com.zephyr.client.module.disable.disableRainEffects;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class NoRainSoundMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At("HEAD"), cancellable = true)
    private void cancelRainSounds(SoundInstance sound, CallbackInfo ci) {
        if (!disableRainEffects.INSTANCE.isEnabled()) return;

        ResourceLocation id = sound.getLocation();

        if (id != null && id.getPath().contains("weather.rain")) {
            ci.cancel();
        }
    }
}
