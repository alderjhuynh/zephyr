package com.zephyr.client.module.qol.seedcracker.cracker.decorator;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcterrain.TerrainGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for seedfinding decorator features (small worldgen-placed features).
 *
 * <p>Extends {@link Feature} and adds version-aware biome validation, a biome-indexed salt map
 * used to derive the decorator seed, and both a seedfinding ({@link ChunkRand}) and a vanilla
 * ({@link WorldgenRandom}) starting path.
 */
public abstract class Decorator<C extends Decorator.Config, D extends Decorator.Data<?>> extends Feature<C, D> {

    public Decorator(C config, MCVersion version) {
        super(config, version);
    }

    /**
     * @param biome the biome the decorator generated in
     * @return the low part of the salt (0-9999) used when deriving the decorator seed
     */
    public int getIndex(Biome biome) {
        return this.getConfig().getSalt(biome) % 10000;
    }

    /**
     * @param biome the biome the decorator generated in
     * @return the high part of the salt (salt / 10000) used when deriving the decorator seed
     */
    public int getStep(Biome biome) {
        return this.getConfig().getSalt(biome) / 10000;
    }

    /**
     * Positions the seedfinding random on the decorator seed for this feature and returns whether
     * generation can start at the recorded chunk.
     *
     * @param data the decorator placement data
     * @param structureSeed the structure/world seed
     * @param rand the seedfinding random to seed
     * @return always true once the seed is set up
     */
    @Override
    public boolean canStart(D data, long structureSeed, ChunkRand rand) {
        rand.setDecoratorSeed(structureSeed, data.chunkX << 4, data.chunkZ << 4,
                this.getIndex(data.biome), this.getStep(data.biome), this.getVersion());
        return true;
    }

    /**
     * Positions the vanilla random on the decorator seed for this feature and returns whether
     * generation can start at the recorded chunk.
     *
     * @param data the decorator placement data
     * @param worldSeed the world seed
     * @param rand the vanilla worldgen random to seed
     * @return always true once the seed is set up
     */
    public boolean canStart(D data, long worldSeed, WorldgenRandom rand) {
        long l = rand.setDecorationSeed(worldSeed, data.chunkX << 4, data.chunkZ << 4);
        rand.setFeatureSeed(l, this.getIndex(data.biome), this.getStep(data.biome));
        return true;
    }

    @Override
    public boolean canGenerate(D data, TerrainGenerator generator) {
        return true;
    }

    @Override
    public final boolean canSpawn(D data, BiomeSource source) {
        return this.canSpawn(data.chunkX, data.chunkZ, source);
    }

    /**
     * Checks whether the given chunk's biome is valid for this decorator, using the biome query
     * appropriate for the feature's version.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param source the biome source to sample
     * @return true if the decorator may spawn in this chunk's biome
     */
    public boolean canSpawn(int chunkX, int chunkZ, BiomeSource source) {
        if (this.getVersion().isOlderThan(MCVersion.v1_16)) {
            return this.isValidBiome(source.getBiome((chunkX << 4) + 8, 0, (chunkZ << 4) + 8));
        }

        return this.isValidBiome(source.getBiomeForNoiseGen((chunkX << 2) + 2, 0, (chunkZ << 2) + 2));
    }

    /**
     * @param biome the biome to validate
     * @return true if this decorator is allowed to generate in the given biome
     */
    public abstract boolean isValidBiome(Biome biome);

    /**
     * Configuration for a {@link Decorator}: a default salt plus optional per-biome salt overrides.
     * The salt encodes the feature's step (high digits) and index (low 4 digits), which together
     * determine the decorator seed.
     */
    public static class Config extends Feature.Config {
        /** Salt used for biomes that have no explicit override. */
        public final int defaultSalt;
        /** Per-biome salt overrides keyed by seedfinding biome. */
        public final Map<Biome, Integer> salts = new HashMap<>();

        public Config(int step, int index) {
            this.defaultSalt = step * 10000 + index;
        }

        /**
         * Registers a salt override for the given biomes.
         *
         * @param step the step part of the salt
         * @param index the index part of the salt
         * @param biomes the biomes to apply the override to
         * @return this config, for chaining
         */
        public Config add(int step, int index, Biome... biomes) {
            for (Biome biome : biomes) {
                this.salts.put(biome, step * 10000 + index);
            }

            return this;
        }

        /**
         * @param biome the biome to look up
         * @return the effective salt for the given biome (falling back to {@link #defaultSalt})
         */
        public int getSalt(Biome biome) {
            return this.salts.getOrDefault(biome, this.defaultSalt);
        }
    }

    /**
     * Placement data for a {@link Decorator}: the chunk coordinates plus the biome the feature was
     * observed in.
     */
    public static class Data<T extends Decorator<?, ?>> extends Feature.Data<T> {
        /** The biome the decorator was found in. */
        public final Biome biome;

        public Data(T feature, int chunkX, int chunkZ, Biome biome) {
            super(feature, chunkX, chunkZ);
            this.biome = biome;
        }

        /**
         * Tests whether the vanilla generation random, seeded with the given world seed, reproduces
         * this placement.
         *
         * @param worldSeed the candidate world seed
         * @param rand the vanilla random to seed
         * @return true if the decorator starts at the recorded chunk
         */
        public boolean testStart(long worldSeed, WorldgenRandom rand) {
            return ((Decorator) this.feature).canStart(this, worldSeed, rand);
        }
    }

}
