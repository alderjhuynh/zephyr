package com.zephyr.client.module.qol.seedcracker.finder.decorator.ore;

import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.EmeraldOre;
import com.zephyr.client.module.qol.seedcracker.finder.BlockFinder;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import com.zephyr.client.module.qol.seedcracker.util.BiomeFixer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds emerald ore veins in mountain biomes.
 *
 * <p>Scans the vertical band where emerald ore generates and records an {@code EmeraldOre.Data}
 * constraint for each vein found.
 */
public class EmeraldOreFinder extends BlockFinder {

    protected static List<BlockPos> SEARCH_POSITIONS;

    public EmeraldOreFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos, Blocks.EMERALD_ORE);
        this.searchPositions = SEARCH_POSITIONS;
    }

    /**
     * Restricts the search to the y range where emerald ore generates (y 4..32).
     */
    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = Finder.buildSearchPositions(Finder.CHUNK_POSITIONS, pos -> {
            if (pos.getY() < 4) return true;
            return pos.getY() > 28 + 4;
        });
    }

    /**
     * @return a single {@link EmeraldOreFinder} for the given chunk
     */
    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EmeraldOreFinder(world, chunkPos));
        return finders;
    }

    /**
     * Scans for emerald ore and records an {@code EmeraldOre.Data} constraint for the vein found.
     *
     * @return the matched ore positions
     */
    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getNoiseBiome((this.chunkPos.x() << 2) + 2, 0, (this.chunkPos.z() << 2) + 2).value();

        List<BlockPos> result = super.findInChunk();
        if (result.isEmpty()) return result;

        BlockPos pos = result.get(0);

        EmeraldOre.Data data = Features.EMERALD_ORE.at(pos.getX(), pos.getY(), pos.getZ(), BiomeFixer.swap(biome));

        if (Seedcracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
            this.cuboids.add(new Cuboid(pos, ARGB.color(0, 255, 0)));
        }

        return result;
    }

    /**
     * @return true for the overworld dimension
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

}
