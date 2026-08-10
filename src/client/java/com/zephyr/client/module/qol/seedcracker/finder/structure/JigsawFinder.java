package com.zephyr.client.module.qol.seedcracker.finder.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link PieceFinder} whose layout is placed using jigsaw-style rotation math.
 *
 * <p>Used for structures whose piece can be rotated per facing (igloos, outposts, trial chambers).
 */
public class JigsawFinder extends PieceFinder {
    public JigsawFinder(Level world, ChunkPos chunkPos, Direction facing, Vec3i size) {
        super(world, chunkPos, facing, size);
    }

    /**
     * Computes the per-facing search positions for a rotated structure piece.
     *
     * @param xRotation rotation offset on the X axis
     * @param zRotation rotation offset on the Z axis
     * @param xOffset X offset of the piece origin
     * @param zOffset Z offset of the piece origin
     * @param size the piece size
     * @return per-facing lists of positions to probe
     */
    public static Map<Direction, List<BlockPos>> getSearchPositions(int xRotation, int zRotation, int xOffset, int zOffset, Vec3i size) {
        Map<Direction, List<BlockPos>> positions = new HashMap<>();

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            positions.put(direction, new ArrayList<>());
            //this was painful and my hope is to never do it again
            int x = switch (direction) {
                case EAST -> xRotation-size.getZ()+1+zRotation-zOffset;
                case SOUTH -> xRotation-size.getX()+1+xRotation-xOffset;
                case WEST -> xRotation-zRotation+zOffset;
                default -> xOffset;
            };
            int z = switch (direction) {
                case EAST -> zRotation-xRotation+xOffset;
                case SOUTH -> zRotation-size.getZ()+1+zRotation-zOffset;
                case WEST -> zRotation-size.getX()+1+xRotation-xOffset;
                default -> zOffset;
            };
            if (x >= 0 && x < 16 && z >= 0 && z < 16 ) {
                int startIndex = heightContext.getHeight()*x*16+heightContext.getHeight()*z;
                for (int y = 0; y < heightContext.getHeight(); y++) {
                    positions.get(direction).add(CHUNK_POSITIONS.get(y+startIndex));
                }
            } else {
                for (int y = heightContext.getBottomY(); y < heightContext.getTopY(); y++) {
                    positions.get(direction).add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }


    /**
     * Sets the piece orientation, mapping each facing to the corresponding jigsaw rotation.
     *
     * @param facing the facing direction (null for no rotation)
     */
    @Override
    public void setOrientation(Direction facing) {
        this.facing = facing;
        this.mirror = Mirror.NONE;
        if (facing == null) {
            this.rotation = Rotation.NONE;
        } else {
            switch (facing) {
                case SOUTH -> this.rotation = Rotation.CLOCKWISE_180;
                case WEST -> this.rotation = Rotation.COUNTERCLOCKWISE_90;
                case EAST -> this.rotation = Rotation.CLOCKWISE_90;
                default -> this.rotation = Rotation.NONE;
            }
        }
    }

    /**
     * Applies the facing-based X transform to a layout position.
     *
     * @param x the layout X
     * @param z the layout Z
     * @return the rotated X coordinate
     */
    @Override
    protected int applyXTransform(int x, int z) {
        if (this.facing == null) {
            return x;
        } else {
            return switch (this.facing) {
                case EAST -> -z + this.getLayout().getX()-1;
                case SOUTH -> -x + this.getLayout().getX()-1;
                case WEST -> z;

                default -> x;
            };
        }
    }

    /**
     * Applies the facing-based Z transform to a layout position.
     *
     * @param x the layout X
     * @param z the layout Z
     * @return the rotated Z coordinate
     */
    @Override
    protected int applyZTransform(int x, int z) {
        if (this.facing == null) {
            return z;
        } else {
            return switch (this.facing) {
                case EAST -> x;
                case SOUTH -> -z + this.getLayout().getZ()-1;
                case WEST -> -x + this.getLayout().getZ()-1;

                default -> z;
            };
        }
    }

}
