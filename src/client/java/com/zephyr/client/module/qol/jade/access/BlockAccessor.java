package com.zephyr.client.module.qol.jade.access;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Describes a targeted block, caching the block state and block entity that were
 * present when the accessor was built (like Jade's {@code BlockAccessor}).
 */
public class BlockAccessor extends Accessor {
    private final Level level;
    private final BlockPos pos;
    private final BlockHitResult hitResult;
    private final BlockState state;
    private final BlockEntity blockEntity;

    /**
     * Builds a block accessor, capturing the current block state and block entity.
     *
     * @param level     the level containing the block
     * @param pos       the position of the targeted block
     * @param hitResult the ray-cast hit result for the block
     */
    public BlockAccessor(Level level, BlockPos pos, BlockHitResult hitResult) {
        this.level = level;
        this.pos = pos;
        this.hitResult = hitResult;
        this.state = level.getBlockState(pos);
        this.blockEntity = level.getBlockEntity(pos);
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public Vec3 getHitLocation() {
        return hitResult.getLocation();
    }

    /** @return the position of the targeted block */
    public BlockPos getPosition() {
        return pos;
    }

    /** @return the underlying block hit result */
    public BlockHitResult getHitResult() {
        return hitResult;
    }

    /** @return the block state at the target when this accessor was built */
    public BlockState getBlockState() {
        return state;
    }

    /** @return the block at the target */
    public Block getBlock() {
        return state.getBlock();
    }

    /** @return the block entity at the target, or null if none exists */
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
