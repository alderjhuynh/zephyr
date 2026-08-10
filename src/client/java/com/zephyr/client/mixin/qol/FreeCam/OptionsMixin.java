package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {
    // Prevents switching to third person in freecam.
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void zephyr$lockPerspective(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && !FreeCam.isSuppressingPerspectiveGuard()) {
            ci.cancel();
        }
    }
}
