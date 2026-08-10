package com.zephyr.client.module.qol.seedcracker.finder;

import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.DesertWellFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.DungeonFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.EndGatewayFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.EndPillarsFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.WarpedFungusFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.ore.EmeraldOreFinder;
import com.zephyr.client.module.qol.seedcracker.finder.structure.*;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import com.zephyr.client.module.qol.seedcracker.util.FeatureToggle;
import com.zephyr.client.module.qol.seedcracker.util.HeightContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base class for all seedcracker finders.
 *
 * <p>A finder scans a single chunk for the fingerprint of one feature type. Concrete subclasses
 * implement {@link #findInChunk()} and collect the results as {@link Cuboid} render boxes plus
 * {@code DataStorage} constraints. {@link Type} enumerates every available finder and binds it to
 * its builder, category and config toggle.
 */
public abstract class Finder {

    /** All in-chunk positions within the current height range, precomputed once per world. */
    protected static final List<BlockPos> CHUNK_POSITIONS = new ArrayList<>();
    /** All 16x16x16 sub-chunk positions, used for scanning full 16-block sub-chunks. */
    protected static final List<BlockPos> SUB_CHUNK_POSITIONS = new ArrayList<>();
    protected static HeightContext heightContext;

    static {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    SUB_CHUNK_POSITIONS.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    protected Minecraft mc = Minecraft.getInstance();
    /** Render boxes for every discovery made by this finder. */
    protected final List<Cuboid> cuboids = new ArrayList<>();
    protected Level world;
    protected ChunkPos chunkPos;

    public Finder(Level world, ChunkPos chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;
    }

    /**
     * Filters a base position list through a removal predicate.
     *
     * @param base the base list of positions
     * @param removeIf positions matching this predicate are dropped
     * @return a new list of kept positions
     */
    public static List<BlockPos> buildSearchPositions(List<BlockPos> base, Predicate<BlockPos> removeIf) {
        List<BlockPos> newList = new ArrayList<>();

        for (BlockPos pos : base) {
            if (!removeIf.test(pos)) {
                newList.add(pos);
            }
        }

        return newList;
    }

    /**
     * @return the level this finder scans
     */
    public Level getWorld() {
        return this.world;
    }

    /**
     * @return the chunk position this finder scans
     */
    public ChunkPos getChunkPos() {
        return this.chunkPos;
    }

    /**
     * Scans this finder's chunk for its feature fingerprint.
     *
     * @return the discovered block positions, in absolute world coordinates
     */
    public abstract List<BlockPos> findInChunk();

    /**
     * @return true if any of this finder's cuboids is within the player's render distance
     */
    public boolean shouldRender() {
        DimensionType finderDim = this.world.dimensionType();
        DimensionType playerDim = mc.player.level().dimensionType();

        if (finderDim != playerDim) return false;

        int renderDistance = mc.options.renderDistance().get() * 16 + 16;
        Vec3 playerPos = mc.player.position();

        for (Cuboid cuboid : this.cuboids) {
            BlockPos pos = cuboid.getCenterPos();
            double distance = playerPos.distanceToSqr(pos.getX(), playerPos.y, pos.getZ());
            if (distance <= renderDistance * renderDistance + 32) return true;
        }

        return false;
    }

    /**
     * @return true if this finder found nothing and can be discarded
     */
    public boolean isUseless() {
        return this.cuboids.isEmpty();
    }

    /**
     * @param dimension the dimension to check
     * @return true if this finder is valid in the given dimension
     */
    public abstract boolean isValidDimension(DimensionType dimension);

    /**
     * @param dimension the dimension to check
     * @return true if the dimension is the overworld
     */
    public boolean isOverworld(DimensionType dimension) {
        return dimension.skybox() == DimensionType.Skybox.OVERWORLD;
    }

    /**
     * @param dimension the dimension to check
     * @return true if the dimension is the nether
     */
    public boolean isNether(DimensionType dimension) {
        return dimension.skybox() == DimensionType.Skybox.NONE;
    }

    /**
     * @param dimension the dimension to check
     * @return true if the dimension is the end
     */
    public boolean isEnd(DimensionType dimension) {
        return dimension.skybox() == DimensionType.Skybox.END;
    }

    /**
     * @param dimension the dimension to check
     * @return the seedfinding dimension id ("overworld", "the_nether" or "the_end")
     */
    public static String inferDimension(DimensionType dimension) {
        return switch (dimension.skybox()) {
            case OVERWORLD -> "overworld";
            case NONE -> "the_nether";
            case END -> "the_end";
        };
    }

    /**
     * Broad category of findable feature types.
     */
    public enum Category {
        STRUCTURES,
        DECORATORS,
        BIOMES,
    }

    /**
     * Enumeration of every finder type available in the module.
     *
     * <p>Each entry binds a {@link FinderBuilder} factory, a {@link Category}, the config toggle
     * controlling whether it is active, and a translation key for the GUI.
     */
    public enum Type {
        BURIED_TREASURE(BuriedTreasureFinder::create, Category.STRUCTURES, Config.get().buriedTreasure, "finder.buriedTreasures"),
        DESERT_TEMPLE(DesertPyramidFinder::create, Category.STRUCTURES, Config.get().desertTemple, "finder.desertTemples"),
        END_CITY(EndCityFinder::create, Category.STRUCTURES, Config.get().endCity, "finder.endCities"),
        JUNGLE_TEMPLE(JunglePyramidFinder::create, Category.STRUCTURES, Config.get().jungleTemple, "finder.jungleTemples"),
        MONUMENT(MonumentFinder::create, Category.STRUCTURES, Config.get().monument, "finder.monuments"),
        SWAMP_HUT(SwampHutFinder::create, Category.STRUCTURES, Config.get().swampHut, "finder.swampHuts"),
        SHIPWRECK(ShipwreckFinder::create, Category.STRUCTURES, Config.get().shipwreck, "finder.shipwrecks"),
        PILLAGER_OUTPOST(OutpostFinder::create, Category.STRUCTURES, Config.get().outpost, "finder.outposts"),
        IGLOO(IglooFinder::create, Category.STRUCTURES, Config.get().igloo, "finder.igloo"),
        TRIAL_CHAMBERS(TrialChambersFinder::create, Category.STRUCTURES, Config.get().trialChambers, "finder.trialChambers"),

        END_PILLARS(EndPillarsFinder::create, Category.DECORATORS, Config.get().endPillars, "finder.endPillars"),
        END_GATEWAY(EndGatewayFinder::create, Category.DECORATORS, Config.get().endGateway, "finder.endGateways"),
        DUNGEON(DungeonFinder::create, Category.DECORATORS, Config.get().dungeon, "finder.dungeons"),
        EMERALD_ORE(EmeraldOreFinder::create, Category.DECORATORS, Config.get().emeraldOre, "finder.emeraldOres"),
        DESERT_WELL(DesertWellFinder::create, Category.DECORATORS, Config.get().desertWell, "finder.desertWells"),
        WARPED_FUNGUS(WarpedFungusFinder::create, Category.DECORATORS, Config.get().warpedFungus, "finder.warpedFungus"),

        BIOME(BiomeFinder::create, Category.BIOMES, Config.get().biome, "finder.biomes");

        public final FinderBuilder finderBuilder;
        public final String nameKey;
        private final Category category;
        public FeatureToggle enabled;

        Type(FinderBuilder finderBuilder, Category category, FeatureToggle enabled, String nameKey) {
            this.finderBuilder = finderBuilder;
            this.category = category;
            this.enabled = enabled;
            this.nameKey = nameKey;
        }

        /**
         * @param category the category to filter by
         * @return all finder types belonging to the given category
         */
        public static List<Type> getForCategory(Category category) {
            return Arrays.stream(values()).filter(type -> type.category == category).collect(Collectors.toList());
        }
    }
}
