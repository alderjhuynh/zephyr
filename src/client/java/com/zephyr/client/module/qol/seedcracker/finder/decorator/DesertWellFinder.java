package com.zephyr.client.module.qol.seedcracker.finder.decorator;

import com.seedfinding.mcfeature.decorator.DesertWell;
import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.finder.structure.PieceFinder;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import com.zephyr.client.module.qol.seedcracker.util.BiomeFixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds desert wells by matching their sandstone/water structure across neighbouring chunks.
 *
 * <p>Because a well can straddle chunk borders, finders are built for the chunk and all eight
 * surrounding chunks. On a match, the well's {@code DesertWell.Data} is pushed as a structure
 * constraint.
 */
public class DesertWellFinder extends PieceFinder {

    protected static Vec3i SIZE = new Vec3i(5, 6, 5);

    public DesertWellFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos, Direction.NORTH, SIZE);
        this.searchPositions = CHUNK_POSITIONS;
        this.buildStructure();
    }

    /**
     * @return desert well finders for the chunk and all eight neighbouring chunks
     */
    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new DesertWellFinder(world, chunkPos));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() - 1, chunkPos.z())));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x(), chunkPos.z() - 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() - 1, chunkPos.z() - 1)));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() + 1, chunkPos.z())));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x(), chunkPos.z() + 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() + 1, chunkPos.z() + 1)));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() + 1, chunkPos.z() - 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x() - 1, chunkPos.z() + 1)));
        return finders;
    }

    /**
     * Scans for the desert well structure and records a {@code DesertWell.Data} constraint for each
     * match found.
     *
     * @return the matched well positions
     */
    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getNoiseBiome((this.chunkPos.x() << 2) + 2, 0, (this.chunkPos.z() << 2) + 2).value();

        if (!Features.DESERT_WELL.isValidBiome(BiomeFixer.swap(biome))) {
            return new ArrayList<>();
        }

        List<BlockPos> result = super.findInChunk();

        result.forEach(pos -> {
            pos = pos.offset(2, 1, 2);

            DesertWell.Data data = Features.DESERT_WELL.at(pos.getX(), pos.getZ());

            if (Seedcracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                this.cuboids.add(new Cuboid(pos.offset(-2, -1, -2), SIZE, ARGB.color(128, 128, 255)));
                this.cuboids.add(new Cuboid(pos, ARGB.color(128, 128, 255)));
            }
        });

        return result;
    }

    /**
     * @return true for the overworld dimension
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

    /**
     * Defines the desert well layout: sandstone base, water reservoir and sandstone slab cap.
     */
    protected void buildStructure() {
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState sandstoneSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        this.fillWithOutline(0, 0, 0, 4, 1, 4, sandstone, sandstone, false);
        this.fillWithOutline(1, 5, 1, 3, 5, 3, sandstoneSlab, sandstoneSlab, false);
        this.addBlock(sandstone, 2, 5, 2);

        BlockPos p1 = new BlockPos(2, 1, 2);
        this.addBlock(water, p1.getX(), p1.getY(), p1.getZ());

        Direction.Plane.HORIZONTAL.forEach(facing -> {
            BlockPos p2 = p1.relative(facing);
            this.addBlock(water, p2.getX(), p2.getY(), p2.getZ());
        });
    }

}
