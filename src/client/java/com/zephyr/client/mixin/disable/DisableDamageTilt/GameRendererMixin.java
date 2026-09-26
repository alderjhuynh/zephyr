package com.zephyr.client.mixin.disable.DisableDamageTilt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableDamageTilt;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link GameRenderer} that backs the {@code disableDamageTilt} module.
 * Cancels {@code GameRenderer.bobHurt} while the module is enabled so the damage-taken
 * camera tilt never renders.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Suppresses the hurt-camera bob when the module is enabled.
     *
     * @param cameraRenderState the camera render state for the current frame
     * @param poseStack         the pose stack the camera transformations are applied to
     * @param ci                mixin callback used to cancel the bob
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableDamageTilt(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        if (disableDamageTilt.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
