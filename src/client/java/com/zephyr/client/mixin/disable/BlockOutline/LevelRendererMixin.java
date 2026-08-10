package com.zephyr.client.mixin.disable.BlockOutline;

import com.zephyr.client.module.disable.disableBlockOutline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideBlockOutline(PoseStack poseStack, VertexConsumer consumer, Entity entity, double x, double y, double z, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!disableBlockOutline.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
