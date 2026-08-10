package com.zephyr.client.mixin.disable.FirstPersonFire;

import com.zephyr.client.module.disable.disableFirstPersonFire;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin targeting {@link ScreenEffectRenderer} that backs the
 * {@code disableFirstPersonFire} module.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    /**
     * Redirects the {@code SubmitNodeCollector.submitCustomGeometry} call inside
     * {@code ScreenEffectRenderer#submitFire} to alter the first-person fire overlay.
     * When the module is disabled the original call is kept; in {@code DISABLE} mode
     * the geometry is skipped entirely, and in {@code LOWER} mode it is submitted
     * shifted down by 0.3 blocks.
     *
     * @param collector  the submit node collector for the overlay geometry
     * @param poseStack  the pose stack for the fire overlay
     * @param renderType the render type for the fire texture
     * @param renderer   the custom geometry renderer producing the fire
     */
    @Redirect(
            method = "submitFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"
            )
    )
    private static void zephyr$handleFirstPersonFire(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer
    ) {
        disableFirstPersonFire module = disableFirstPersonFire.INSTANCE;
        if (!module.isEnabled()) {
            collector.submitCustomGeometry(poseStack, renderType, renderer);
            return;
        }

        switch (module.getMode()) {
            case DISABLE -> {
            }
            case LOWER -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, -0.3F, 0.0F);
                collector.submitCustomGeometry(poseStack, renderType, renderer);
                poseStack.popPose();
            }
        }
    }
}
