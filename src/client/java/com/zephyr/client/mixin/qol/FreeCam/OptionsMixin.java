package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link Options} that locks the camera perspective while the Zephyr
 * FreeCam module is active.
 *
 * <p>Injects at the head of {@code Options.setCameraType} and cancels any
 * perspective change, preventing the player from switching to third person
 * mid-freecam. The module's {@code onEnable} temporarily suppresses this guard
 * via {@link FreeCam#isSuppressingPerspectiveGuard()} so it can force
 * first-person when toggled on.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    /**
     * Cancels {@code setCameraType} while FreeCam is enabled, unless the
     * module is currently forcing first-person.
     *
     * @param ci mixin callback used to cancel the perspective change
     */
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void zephyr$lockPerspective(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && !FreeCam.isSuppressingPerspectiveGuard()) {
            ci.cancel();
        }
    }
}
