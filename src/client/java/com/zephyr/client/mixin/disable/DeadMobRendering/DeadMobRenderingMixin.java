package com.zephyr.client.mixin.disable.DeadMobRendering;

import com.zephyr.client.module.disable.disableDeadMobRendering;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link LivingEntityRenderer} that backs the
 * {@code disableDeadMobRendering} module.
 */
@Mixin(LivingEntityRenderer.class)
public class DeadMobRenderingMixin {

    /**
     * Cancels {@code LivingEntityRenderer#submit} at its head for entities currently
     * in their death animation (positive {@code deathTime}) so that dead mobs are not
     * rendered while the module is enabled.
     *
     * @param state       the living entity render state for the entity
     * @param matrices    the pose stack for rendering
     * @param queue       the submit node collector for the entity geometry
     * @param cameraState the camera render state for the current frame
     * @param ci          the cancellable injection callback
     */
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void cancelDeadMobRendering(
            LivingEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!disableDeadMobRendering.INSTANCE.isEnabled()) return;
        if (state != null && state.deathTime > 0.0F) {
            ci.cancel();
        }
    }
}
