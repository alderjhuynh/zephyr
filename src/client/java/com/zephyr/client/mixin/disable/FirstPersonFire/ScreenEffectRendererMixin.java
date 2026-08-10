package com.zephyr.client.mixin.disable.FirstPersonFire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableFirstPersonFire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow
    private static void renderFire(Minecraft minecraft, PoseStack poseStack) {
    }

    @Redirect(
            method = "renderScreenEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private static void zephyr$handleFirstPersonFire(Minecraft minecraft, PoseStack poseStack) {
        disableFirstPersonFire module = disableFirstPersonFire.INSTANCE;
        if (!module.isEnabled()) {
            renderFire(minecraft, poseStack);
            return;
        }

        switch (module.getMode()) {
            case DISABLE -> {
            }
            case LOWER -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, -0.3F, 0.0F);
                renderFire(minecraft, poseStack);
                poseStack.popPose();
            }
        }
    }
}
