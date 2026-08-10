package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link Hud} keeping HUD elements tied to the real player while
 * the Zephyr FreeCam module is active.
 *
 * <p>Makes HUD lookup report the local player instead of the FreeCamera entity
 * (so health, food, etc. are correct) and suppresses texture overlays such as
 * the pumpkin overlay while the camera is detached.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
    /**
     * Makes {@code Hud.getCameraPlayer} return the local player whenever FreeCam
     * is active, so the HUD reflects the player rather than the FreeCamera.
     *
     * @param cir mixin callback used to override the returned player
     */
    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void zephyr$cameraPlayer(CallbackInfoReturnable<Player> cir) {
        if (FreeCam.INSTANCE.isEnabled()) {
            cir.setReturnValue(MC.player);
        }
    }

    /**
     * Cancels HUD texture overlays (e.g. the pumpkin blur) while FreeCam is
     * active so they are not rendered from the detached camera.
     *
     * @param graphics the HUD graphics context
     * @param texture  the texture being drawn as an overlay
     * @param alpha    the overlay opacity
     * @param ci       mixin callback used to cancel the overlay render
     */
    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$textureOverlay(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            float alpha,
            CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
