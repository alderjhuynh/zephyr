package com.zephyr.client.mixin.disable.RainEffects;

import com.zephyr.client.module.disable.disableRainEffects;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link SoundManager} that backs the {@code disableRainEffects}
 * module. It handles the audio half of the module's behaviour.
 */
@Mixin(SoundManager.class)
public class NoRainSoundMixin {

    /**
     * Intercepts {@code SoundManager#play} at its head and blocks any sound whose
     * identifier path contains {@code weather.rain} from being started while the module
     * is enabled, reporting the sound as not started.
     *
     * @param sound the sound instance about to be played
     * @param cir   the cancellable return-value callback
     */
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"), cancellable = true)
    private void cancelRainSounds(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        Identifier id = sound.getIdentifier();

        if  (!disableRainEffects.INSTANCE.isEnabled()) return;

        if (id != null && (
                id.getPath().contains("weather.rain")
        )) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
