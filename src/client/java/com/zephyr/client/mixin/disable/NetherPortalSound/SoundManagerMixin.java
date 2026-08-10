package com.zephyr.client.mixin.disable.NetherPortalSound;

import com.zephyr.client.module.disable.disableNetherPortalSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelNetherPortalSound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null || sound.getLocation() == null) return;

        ResourceLocation id = sound.getLocation();

        if (id.equals(ResourceLocation.withDefaultNamespace("block.portal.ambient"))) {
            if (!disableNetherPortalSound.INSTANCE.isEnabled()) return;
            ci.cancel();
        }
    }
}
