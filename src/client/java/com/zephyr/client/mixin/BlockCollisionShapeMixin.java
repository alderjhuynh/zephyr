package com.zephyr.client.mixin;

import com.zephyr.client.Jesus;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class BlockCollisionShapeMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void onCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context,
                                  CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape shape = Jesus.getCollisionShape(state, pos, context);
        if (shape != null) {
            cir.setReturnValue(shape);
        }
    }
}
