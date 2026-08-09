package com.zephyr.client.mixin.disable.FirstPersonFire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableFirstPersonFire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @WrapOperation(
            method = "renderScreenEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            )
    )
    private static void zephyr$handleFirstPersonFire(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            Operation<Void> original
    ) {
        disableFirstPersonFire module = disableFirstPersonFire.INSTANCE;
        if (!module.isEnabled()) {
            original.call(poseStack, bufferSource, sprite);
            return;
        }

        switch (module.getMode()) {
            case DISABLE -> {
            }
            case LOWER -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, -0.3F, 0.0F);
                original.call(poseStack, bufferSource, sprite);
                poseStack.popPose();
            }
        }
    }
}
