package com.zephyr.client.module.qol.seedcracker.finder.structure;

import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches a fixed structure layout against the world at a set of probe positions.
 *
 * <p>A piece is described as a map of local {@link BlockState}s built through {@link #addBlock}
 * and {@link #fillWithOutline}. {@link #findInChunk()} slides the layout over every search
 * position and reports the origins where all blocks match.
 */
public class PieceFinder extends Finder {

    private final BoundingBox boundingBox;
    /** The structure layout: local position to expected block state. */
    protected Map<BlockPos, BlockState> structure = new LinkedHashMap<>();
    /** Positions to probe for a layout match. */
    protected List<BlockPos> searchPositions = new ArrayList<>();

    protected Direction facing;
    protected int width;
    protected int height;
    protected int depth;
    protected Mirror mirror;
    protected Rotation rotation;
    private boolean debug;

    public PieceFinder(Level world, ChunkPos chunkPos, Direction facing, Vec3i size) {
        super(world, chunkPos);

        this.setOrientation(facing);
        this.width = size.getX();
        this.height = size.getY();
        this.depth = size.getZ();

        if (this.facing.getAxis() == Direction.Axis.Z) {
            this.boundingBox = new BoundingBox(
                    0, 0, 0,
                    size.getX() - 1, size.getY() - 1, size.getZ() - 1
            );
        } else {
            this.boundingBox = new BoundingBox(
                    0, 0, 0,
                    size.getZ() - 1, size.getY() - 1, size.getX() - 1
            );
        }
    }

    /**
     * @return the layout dimensions after applying the piece's facing
     */
    public Vec3i getLayout() {
        if (this.facing.getAxis() != Direction.Axis.Z) {
            return new Vec3i(this.depth, this.height, this.width);
        }

        return new Vec3i(this.width, this.height, this.depth);
    }

    /**
     * Slides the structure layout over every search position and returns matching origins.
     *
     * @return the matched positions, in absolute world coordinates
     */
    @Override
    public List<BlockPos> findInChunk() {
        List<BlockPos> result = new ArrayList<>();

        if (this.structure.isEmpty()) {
            return result;
        }

        //FOR DEBUGGING PIECES.
        if (this.debug) {
            Minecraft.getInstance().execute(() -> {
                int y = this.rotation.ordinal() * (this.height + 5) + this.mirror.ordinal() * (this.height + 5) * 2 + 120;

                if (this.chunkPos.x() % 2 == 0 && this.chunkPos.z() % 2 == 0) {
                    this.structure.forEach((pos, state) -> {
                        this.world.setBlock(this.chunkPos.getWorldPosition().offset(pos).offset(0, y, 0), state, 0);
                    });
                }
            });
        }

        for (BlockPos center : this.searchPositions) {
            boolean found = true;

            for (Map.Entry<BlockPos, BlockState> entry : this.structure.entrySet()) {
                BlockPos pos = this.chunkPos.getWorldPosition().offset(center.offset(entry.getKey()));
                BlockState state = this.world.getBlockState(pos);

                //Blockstate may change when it gets placed in the world, that's why it's using the block here.
                if (entry.getValue() != null && !state.getBlock().equals(entry.getValue().getBlock())) {
                    found = false;
                    break;
                }
            }

            if (found) {
                result.add(this.chunkPos.getWorldPosition().offset(center));
            }
        }

        return result;
    }

    /**
     * Sets the piece orientation: facing direction plus the matching mirror and rotation.
     *
     * @param facing the facing direction (null for none)
     */
    public void setOrientation(Direction facing) {
        this.facing = facing;

        if (facing == null) {
            this.rotation = Rotation.NONE;
            this.mirror = Mirror.NONE;
        } else {
            switch (facing) {
                case SOUTH:
                    this.mirror = Mirror.LEFT_RIGHT;
                    this.rotation = Rotation.NONE;
                    break;
                case WEST:
                    this.mirror = Mirror.LEFT_RIGHT;
                    this.rotation = Rotation.CLOCKWISE_90;
                    break;
                case EAST:
                    this.mirror = Mirror.NONE;
                    this.rotation = Rotation.CLOCKWISE_90;
                    break;
                default:
                    this.mirror = Mirror.NONE;
                    this.rotation = Rotation.NONE;
            }
        }

    }

    /**
     * Maps a layout (x, z) to its bounding-box X coordinate for this facing.
     *
     * @param x the layout X
     * @param z the layout Z
     * @return the transformed X coordinate
     */
    protected int applyXTransform(int x, int z) {
        if (this.facing == null) {
            return x;
        } else {
            return switch (this.facing) {
                case NORTH, SOUTH -> this.boundingBox.minX() + x;
                case WEST -> this.boundingBox.maxX() - z;
                case EAST -> this.boundingBox.minX() + z;
                default -> x;
            };
        }
    }

    /**
     * Maps a layout Y to its bounding-box Y coordinate for this facing.
     *
     * @param y the layout Y
     * @return the transformed Y coordinate
     */
    protected int applyYTransform(int y) {
        return this.facing == null ? y : y + this.boundingBox.minY();
    }

    /**
     * Maps a layout (x, z) to its bounding-box Z coordinate for this facing.
     *
     * @param x the layout X
     * @param z the layout Z
     * @return the transformed Z coordinate
     */
    protected int applyZTransform(int x, int z) {
        if (this.facing == null) {
            return z;
        } else {
            return switch (this.facing) {
                case NORTH -> this.boundingBox.maxZ() - z;
                case SOUTH -> this.boundingBox.minZ() + z;
                case WEST, EAST -> this.boundingBox.minZ() + x;
                default -> z;
            };
        }
    }

    /**
     * Returns the block state at a layout position, defaulting to air outside the bounding box.
     *
     * @param ox the layout X
     * @param oy the layout Y
     * @param oz the layout Z
     * @return the expected block state
     */
    protected BlockState getBlockAt(int ox, int oy, int oz) {
        int x = this.applyXTransform(ox, oz);
        int y = this.applyYTransform(oy);
        int z = this.applyZTransform(ox, oz);
        BlockPos pos = new BlockPos(x, y, z);

        return !this.boundingBox.isInside(pos) ?
                Blocks.AIR.defaultBlockState() :
                this.structure.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * Fills a box with an outline block and an interior block.
     *
     * @param minX the minimum X
     * @param minY the minimum Y
     * @param minZ the minimum Z
     * @param maxX the maximum X
     * @param maxY the maximum Y
     * @param maxZ the maximum Z
     * @param outline the block placed on the box surface
     * @param inside the block placed inside the box (null to leave empty)
     * @param onlyReplaceAir whether non-air positions should be left untouched
     */
    protected void fillWithOutline(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState outline, BlockState inside, boolean onlyReplaceAir) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (!onlyReplaceAir || !this.getBlockAt(x, y, z).isAir()) {
                        if (y != minY && y != maxY && x != minX && x != maxX && z != minZ && z != maxZ) {
                            this.addBlock(inside, x, y, z);
                        } else {
                            this.addBlock(outline, x, y, z);
                        }
                    }
                }
            }
        }

    }

    /**
     * Adds a block to the layout, applying the piece's mirror and rotation to the state.
     *
     * @param state the block state to add (null removes the position)
     * @param x the layout X
     * @param y the layout Y
     * @param z the layout Z
     */
    protected void addBlock(BlockState state, int x, int y, int z) {
        BlockPos pos = new BlockPos(
                this.applyXTransform(x, z),
                this.applyYTransform(y),
                this.applyZTransform(x, z)
        );

        if (this.boundingBox.isInside(pos)) {
            if (state == null) {
                this.structure.remove(pos);
                return;
            }

            if (this.mirror != Mirror.NONE) {
                state = state.mirror(this.mirror);
            }

            if (this.rotation != Rotation.NONE) {
                state = state.rotate(this.rotation);
            }


            this.structure.put(pos, state);
        }
    }

    /**
     * @return always true (piece finders decide validity via their owning finder)
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return true;
    }

    /**
     * Enables debug mode, which materialises the layout in-world for visual inspection.
     */
    public void setDebug() {
        this.debug = true;
    }

}
