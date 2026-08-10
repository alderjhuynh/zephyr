package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link GameRenderer} providing two FreeCam rendering tweaks:
 * suppressing the first-person hand render and disabling the block outline
 * while interactions are blocked.
 *
 * <p>Backs the Zephyr FreeCam module's "Show Hand" and "Allow Interactions"
 * settings.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Cancels the in-hand item render when FreeCam's "Show Hand" setting is
     * disabled, so the detached camera has a clear view.
     *
     * @param ci mixin callback used to cancel the render
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideHand(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldHideHand()) {
            ci.cancel();
        }
    }

    /**
     * Disables the block outline while FreeCam is active and interactions are
     * prevented (i.e. "Allow Interactions" is off).
     *
     * @param cir mixin callback used to return {@code false}
     */
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && !FreeCam.isPlayerControlEnabled() && FreeCam.shouldPreventInteractions()) {
            cir.setReturnValue(false);
        }
    }
}
