package com.zephyr.client.mixin.disable.FireRendering;

import com.zephyr.client.module.disable.disableFireRendering;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backs the {@link disableFireRendering} module (world fire blocks).
 *
 * <p>Cancels {@code ModelBlockRenderer#tesselateBlock} at its head for any
 * {@link BaseFireBlock} (covering fire and soul fire) so no fire quads are
 * ever emitted into the chunk mesh while enabled. Cancelling the whole block
 * covers every quad path (flat, ambient occlusion, or any future one).
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void zephyr$skipFireBlock(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed,
            CallbackInfo ci) {
        if (disableFireRendering.INSTANCE.isEnabled() && state.getBlock() instanceof BaseFireBlock) {
            ci.cancel();
        }
    }
}
