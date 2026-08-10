package com.zephyr.client.module.qol.seedcracker.finder.structure;

import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for temple structures that are matched by scanning a probe line through the chunk.
 *
 * <p>For each horizontal facing a {@link PieceFinder} is built and configured with a search line
 * (x/z = 0 positions only, since temples are probed from their base). Subclasses supply the temple
 * layout via {@link #buildStructure(PieceFinder)}.
 */
public abstract class AbstractTempleFinder extends Finder {

    protected static List<BlockPos> SEARCH_POSITIONS;
    /** The temple's overall size. */
    protected final Vec3i size;
    protected List<PieceFinder> finders = new ArrayList<>();

    public AbstractTempleFinder(Level world, ChunkPos chunkPos, Vec3i size) {
        super(world, chunkPos);

        Direction.Plane.HORIZONTAL.forEach(direction -> {
            PieceFinder finder = new PieceFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS;

            buildStructure(finder);
            this.finders.add(finder);
        });

        this.size = size;
    }

    /**
     * Restricts the temple search to the vertical probe line (x = z = 0, y 0..200).
     */
    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
            if (pos.getX() != 0) return true;
            if (pos.getY() < 0) return true;
            if (pos.getY() > 200) return true;
            return pos.getZ() != 0;
        });
    }

    /**
     * Runs a single piece finder against the chunk, bailing early when the biome is invalid.
     *
     * @param pieceFinder the piece finder to run
     * @return the matched positions for that piece
     */
    public List<BlockPos> findInChunkPiece(PieceFinder pieceFinder) {
        Biome biome = this.world.getNoiseBiome((this.chunkPos.x() << 2) + 2, 64, (this.chunkPos.z() << 2) + 2).value();

        if (!isValidBiome(biome)) {
            return new ArrayList<>();
        }

        return pieceFinder.findInChunk();
    }

    /**
     * @param biome the biome to validate
     * @return true if this temple may generate in the given biome
     */
    protected abstract boolean isValidBiome(Biome biome);

    /**
     * Adds render boxes for a matched temple: one for the structure layout and one marking the
     * chunk start position.
     *
     * @param pieceFinder the piece finder that matched
     * @param origin the matched origin position
     * @param argb the outline color
     */
    public void addRenderers(PieceFinder pieceFinder, BlockPos origin, int argb) {
        this.cuboids.add(new Cuboid(origin, pieceFinder.getLayout(), argb));
        BlockPos chunkStart = new BlockPos(origin.getX() & -16, origin.getY(), origin.getZ() & -16);
        this.cuboids.add(new Cuboid(chunkStart, argb));
    }

    /**
     * @return a map of every piece finder to its matched positions
     */
    public Map<PieceFinder, List<BlockPos>> findInChunkPieces() {
        Map<PieceFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, this.findInChunkPiece(pieceFinder));
        });

        return result;
    }

    /**
     * Defines the temple layout on the given piece finder.
     *
     * @param finder the piece finder to build the structure on
     */
    public abstract void buildStructure(PieceFinder finder);

    /**
     * @return true for the overworld dimension
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }
}
