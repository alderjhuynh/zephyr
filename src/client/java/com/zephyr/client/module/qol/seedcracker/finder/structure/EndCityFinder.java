package com.zephyr.client.module.qol.seedcracker.finder.structure;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds end cities by matching the characteristic purpur/end-stone-brick doorway structure.
 *
 * <p>Since 1.19 end cities shifted by one block, an offset is applied to the recorded chunk
 * coordinates for those versions.
 */
public class EndCityFinder extends Finder {

    protected static List<BlockPos> SEARCH_POSITIONS;
    protected final Vec3i size = new Vec3i(8, 4, 8);
    protected List<PieceFinder> finders = new ArrayList<>();

    public EndCityFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos);

        Direction.Plane.HORIZONTAL.forEach(direction -> {
            PieceFinder finder = new PieceFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS;

            buildStructure(finder);
            this.finders.add(finder);
        });
    }

    /**
     * Restricts the search to the y band where end cities generate (y 40..90).
     */
    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> pos.getY() > 90 && pos.getY() < 40);
    }

    /**
     * @return end city finders for the chunk and its west/south neighbours
     */
    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndCityFinder(world, chunkPos));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x() - 1, chunkPos.z())));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x(), chunkPos.z() - 1)));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x() - 1, chunkPos.z() - 1)));
        return finders;
    }

    /**
     * Defines the end city tower base layout: end stone brick walls, purpur floor and purple glass
     * windows.
     *
     * @param finder the piece finder to build the structure on
     */
    private void buildStructure(PieceFinder finder) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState endstoneBricks = Blocks.END_STONE_BRICKS.defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState purpurPillar = Blocks.PURPUR_PILLAR.defaultBlockState();
        BlockState purpleGlass = Blocks.STAINED_GLASS.magenta().defaultBlockState();

        //Walls
        finder.fillWithOutline(0, 0, 0, 7, 4, 7, endstoneBricks, null, false);

        //Wall sides
        finder.fillWithOutline(0, 0, 0, 0, 3, 0, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(7, 0, 0, 7, 3, 0, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(0, 0, 7, 0, 3, 7, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(7, 0, 7, 7, 3, 7, purpurPillar, purpurPillar, false);

        //Floor
        finder.fillWithOutline(0, 0, 0, 7, 0, 7, purpur, purpur, false);

        //Doorway
        finder.fillWithOutline(3, 1, 0, 4, 3, 0, air, air, false);

        //Windows
        finder.fillWithOutline(0, 2, 2, 0, 3, 2, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(0, 2, 5, 0, 3, 5, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(7, 2, 2, 7, 3, 2, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(7, 2, 5, 7, 3, 5, purpleGlass, purpleGlass, false);
    }

    /**
     * Runs all piece finders and records an {@code EndCity.Data} constraint for each match,
     * applying the 1.19 position offset when needed.
     *
     * @return the combined matched positions
     */
    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getNoiseBiome((this.chunkPos.x() << 2) + 2, 64, (this.chunkPos.z() << 2) + 2).value();
        if (!Features.END_CITY.isValidBiome(BiomeFixer.swap(biome))) return new ArrayList<>();

        Map<PieceFinder, List<BlockPos>> result = this.findInChunkPieces();
        List<BlockPos> combinedResult = new ArrayList<>();

        result.forEach((pieceFinder, positions) -> {
            positions.removeIf(pos -> {
                //Figure this out, it's not a trivial task.
                return false;
            });

            combinedResult.addAll(positions);

            positions.forEach(pos -> {
                //minecraft 1.19 moved end citys by 1 block reeeeeeeee
                if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_19)) {
                    BlockPos posFix = pos.offset(1, 0, 1);
                    RegionStructure.Data<?> data = Features.END_CITY.at(posFix.getX()>>4, posFix.getZ()>>4);

                    if (Seedcracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                        this.cuboids.add(new Cuboid(pos, pieceFinder.getLayout(), ARGB.color(153, 0, 153)));
                        this.cuboids.add(new Cuboid(posFix, ARGB.color(153, 0, 153)));
                    }
                } else {
                    RegionStructure.Data<?> data = Features.END_CITY.at(this.chunkPos.x(), this.chunkPos.z());

                    if (Seedcracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                        this.cuboids.add(new Cuboid(pos, pieceFinder.getLayout(), ARGB.color(153, 0, 153)));
                        this.cuboids.add(new Cuboid(pos, ARGB.color(153, 0, 153)));
                    }
                }
            });
        });

        return combinedResult;
    }

    /**
     * @return a map of every piece finder to its matched positions
     */
    public Map<PieceFinder, List<BlockPos>> findInChunkPieces() {
        Map<PieceFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, pieceFinder.findInChunk());
        });

        return result;
    }

    /**
     * @return true for the End dimension
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

}
