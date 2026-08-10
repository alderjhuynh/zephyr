package com.zephyr.client.mixin.disable.DeadMobRendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableDeadMobRendering;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class DeadMobRenderingMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void cancelDeadMobRendering(LivingEntity livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (!disableDeadMobRendering.INSTANCE.isEnabled()) return;
        if (livingEntity.deathTime > 0) {
            ci.cancel();
        }
    }
}
