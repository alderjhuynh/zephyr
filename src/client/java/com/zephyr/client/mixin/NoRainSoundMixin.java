package com.zephyr.client.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableRainEffects;

@Mixin(SoundManager.class)
public class NoRainSoundMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At("HEAD"), cancellable = true)
    private void cancelRainSounds(SoundInstance sound, CallbackInfo ci) {
        Identifier id = sound.getId();

        if  (!disableRainEffects.enabled) return;

        if (id != null && (
                id.getPath().contains("weather.rain")
        )) {
            ci.cancel();
        }
    }
}