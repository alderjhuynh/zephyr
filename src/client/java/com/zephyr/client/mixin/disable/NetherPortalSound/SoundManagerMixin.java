package com.zephyr.client.mixin.disable.NetherPortalSound;

import com.zephyr.client.module.disable.disableNetherPortalSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelNetherPortalSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound == null || sound.getIdentifier() == null) return;

        Identifier id = sound.getIdentifier();

        if (id.equals(Identifier.withDefaultNamespace("block.portal.ambient"))) {
            if (!disableNetherPortalSound.INSTANCE.isEnabled()) return;
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
