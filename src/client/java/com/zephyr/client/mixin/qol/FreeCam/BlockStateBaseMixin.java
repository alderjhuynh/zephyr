package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link BlockBehaviour.BlockStateBase} that removes block collision
 * shapes for the Zephyr FreeCam module's {@link FreeCamera} entity.
 *
 * <p>Injects at the head of the 3-arg {@code getCollisionShape} overload. When
 * the collision context belongs to the FreeCamera and the module's
 * "Ignore Collision" setting is on, the shape is replaced with
 * {@link Shapes#empty()} so the camera can fly through blocks.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    /**
     * Shadows {@link BlockBehaviour.BlockStateBase#getBlock()} so the affected
     * block can be passed to the FreeCam collision filter.
     *
     * @return the block this state belongs to
     */
    @Shadow
    public abstract Block getBlock();

    /**
     * Returns an empty collision shape when the collision context is the
     * FreeCamera entity and FreeCam's "Ignore Collision" setting is enabled.
     *
     * @param world   the block getter used for the shape query
     * @param pos     the position of the block state
     * @param context the collision context (checked for the FreeCamera entity)
     * @param cir     mixin callback used to replace the returned shape
     */
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamCollision(BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityShapeContext
                && entityShapeContext.getEntity() instanceof FreeCamera
                && FreeCam.INSTANCE.isEnabled()) {

            if (FreeCam.ignoreCollisionWith(getBlock())) {
                cir.setReturnValue(Shapes.empty());
            }
        }
    }
}
