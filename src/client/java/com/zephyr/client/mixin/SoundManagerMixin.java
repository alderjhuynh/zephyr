package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableNetherPortalSound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelNetherPortalSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (sound == null || sound.getId() == null) return;

        Identifier id = sound.getId();

        if (id.equals(SoundEvents.BLOCK_PORTAL_AMBIENT.id())) {
            if (!disableNetherPortalSound.enabled) return;
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }
}
