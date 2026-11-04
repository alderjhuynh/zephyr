package com.zephyr.client.module.qol.xray.seed;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A minimal {@link ChunkGenerator} used only by the {@code PlacementContext} during
 * seed-based ore prediction on the client.
 *
 * <p>Only surface-level metadata matters for ore features: {@code getGenDepth()},
 * {@code getMinY()} and {@code getSeaLevel()} (via the wrapped {@link GenView}) plus
 * {@link ChunkGenerator#getBiomeGenerationSettings(Holder)} which drives vanilla's
 * {@code BiomeFilter}. Everything else (terrain building, structure placement, mob
 * spawning) is out of the prediction path and fails fast.
 */
final class StubChunkGenerator extends ChunkGenerator {

    private final GenAccess view;

    /**
     * @param view          the read-mostly world facade
     * @param biomeRemapper maps a biome holder into the predictor's worldgen source
     *                      (synced or built-in vanilla) before its generation
     *                      settings are read — servers may sync biomes stripped of
     *                      their feature lists, which would reject every placement
     */
    StubChunkGenerator(GenAccess view, UnaryOperator<Holder<Biome>> biomeRemapper) {
        // A generator over "no biomes"; nothing in the ore placement path reads it.
        super(new BiomeSource() {
            @Override
            public MapCodec<? extends BiomeSource> codec() {
                return null;
            }

            @Override
            protected Stream<Holder<Biome>> collectPossibleBiomes() {
                return Stream.empty();
            }

            @Override
            public BiomeResolver createResolver(Climate.Sampler sampler) {
                return null;
            }
        }, biome -> biomeRemapper.apply(biome).value().getGenerationSettings());
        this.view = view;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return null;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        throw new UnsupportedOperationException("ore prediction never spawns mobs");
    }

    @Override
    public int getGenDepth() {
        return this.view.getMaxY() - this.view.getMinY();
    }

    @Override
    public int getSeaLevel() {
        return this.view.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return this.view.getMinY();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        throw new UnsupportedOperationException("ore prediction never samples base height");
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        throw new UnsupportedOperationException("ore prediction never samples base columns");
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos feetPos, SamplerContext context) {
        // not a real generator: nothing to report
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
        throw new UnsupportedOperationException("ore prediction never builds terrain");
    }
}
