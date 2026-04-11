package com.zephyr.client.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableNetherPortalSound;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelNetherPortalSound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null || sound.getId() == null) return;

        Identifier id = sound.getId();

        if (id.equals(SoundEvents.BLOCK_PORTAL_AMBIENT.getId())) {
            if (!disableNetherPortalSound.enabled) return;
            ci.cancel();
        }
    }
}