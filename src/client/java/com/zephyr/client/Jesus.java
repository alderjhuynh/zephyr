package com.zephyr.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.entity.LivingEntity;


/*
* Genuinely this is so bad please for the love of god don't use this
 */

public class Jesus {
    public static boolean enabled = false;

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null || mc.world == null) return;

        PlayerEntity player = mc.player;

        if (player.getAbilities().flying || player.isSpectator()) return;

        Entity entity = player.hasVehicle() ? player.getVehicle() : player;

        if (entity == null) return;

        if (entity.isTouchingWater() || entity.isInLava() && !entity.isInSneakingPose()) {
            entity.setVelocity(entity.getVelocity().x, 0, entity.getVelocity().z);
            return;
        }

        BlockPos below = entity.getBlockPos().down();
        BlockState state = mc.world.getBlockState(below);

        if (isLiquid(state)) {
            entity.setVelocity(entity.getVelocity().x, 0, entity.getVelocity().z);
        }
    }

    public static VoxelShape getCollisionShape(BlockState state, BlockPos pos, ShapeContext context) {
        if (!enabled || !(context instanceof EntityShapeContext entityShapeContext)) return null;

        Entity entity = entityShapeContext.getEntity();
        if (entity == null) return null;

        if (entity instanceof PlayerEntity player) {
            if (player.isSpectator()) return null;

            if (isLiquid(state)) {
                if (pos.getY() < player.getY() && !player.isTouchingWater() && !player.isInLava()) {
                    return VoxelShapes.fullCube();
                }
            }
        }

        return null;
    }

    private static boolean isLiquid(BlockState state) {
        return state.isOf(Blocks.WATER)
                || state.isOf(Blocks.LAVA)
                || state.getFluidState().getFluid() == Fluids.WATER
                || state.getFluidState().getFluid() == Fluids.LAVA;
    }
}
