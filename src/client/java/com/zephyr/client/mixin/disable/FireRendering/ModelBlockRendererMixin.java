package com.zephyr.client.mixin.disable.FireRendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zephyr.client.module.disable.disableFireRendering;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's ModelBlockRendererMixin. 1.21.1 tesselateBlock has the
// classic (BlockAndTintGetter, BakedModel, BlockState, BlockPos, PoseStack,
// VertexConsumer, boolean, RandomSource, long, int) signature; 26.3 uses the
// new (BlockQuadOutput, BlockStateModel, ...) render system.
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void zephyr$skipFireBlock(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int overlay,
            CallbackInfo ci) {
        if (disableFireRendering.INSTANCE.isEnabled() && state.getBlock() instanceof BaseFireBlock) {
            ci.cancel();
        }
    }
}
