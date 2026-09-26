package com.zephyr.client.module.qol.seedcracker.cracker.decorator;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mccore.version.VersionMap;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * Deep dungeon decorator (1.18+ deep-slate dungeons).
 *
 * <p>Models the deep dungeon placement attempt: four candidate positions are rolled per chunk, and
 * a match is confirmed when one of them equals the observed block offset.
 */
public class DeepDungeon extends Decorator<Decorator.Config, DeepDungeon.Data> {

    public static final VersionMap<Config> CONFIGS = new VersionMap<Decorator.Config>()
            .add(MCVersion.v1_18, new Decorator.Config(3, 3));

    public DeepDungeon(MCVersion version) {
        super(CONFIGS.getAsOf(version), version);
    }

    @Override
    public String getName() {
        return "dungeon";
    }

    @Override
    public boolean canStart(DeepDungeon.Data data, long structureSeed, ChunkRand rand) {
        return true;
    }

    /**
     * Rolls up to four candidate dungeon positions in the chunk and returns whether one matches the
     * observed block offset.
     *
     * @param data the observed deep dungeon placement
     * @param worldSeed the candidate world seed
     * @param rand the vanilla random already seeded on the decorator seed
     * @return true if a rolled position matches the observed one
     */
    @Override
    public boolean canStart(DeepDungeon.Data data, long worldSeed, WorldgenRandom rand) {
        super.canStart(data, worldSeed, rand);
        int x, y, z;

        for (int i = 0; i < 4; i++) {

            x = rand.nextInt(16);
            z = rand.nextInt(16);
            y = rand.nextInt(58)-58;

            if (y == data.blockY && x == data.offsetX && z == data.offsetZ) {
                return true;
            }

            rand.nextInt(2);
            rand.nextInt(2);
        }
        return false;
    }

    @Override
    public boolean isValidDimension(Dimension dimension) {
        return dimension == Dimension.OVERWORLD;
    }

    @Override
    public Dimension getValidDimension() {
        return Dimension.OVERWORLD;
    }

    /**
     * @param biome the biome to validate
     * @return true only for the End (all non-End biomes are rejected)
     */
    @Override
    public boolean isValidBiome(Biome biome) {
        return biome != Biomes.NETHER_WASTES && biome != Biomes.SOUL_SAND_VALLEY && biome != Biomes.WARPED_FOREST
                && biome != Biomes.CRIMSON_FOREST && biome != Biomes.BASALT_DELTAS && biome != Biomes.END_MIDLANDS
                && biome != Biomes.END_HIGHLANDS && biome != Biomes.END_BARRENS && biome != Biomes.SMALL_END_ISLANDS
                && biome != Biomes.THE_VOID && biome == Biomes.THE_END;
    }

    /**
     * Builds deep dungeon placement data from a block position.
     *
     * @param blockX the observed block X
     * @param blockY the observed block Y
     * @param blockZ the observed block Z
     * @param biome the biome the dungeon was found in
     * @return the placement data
     */
    public DeepDungeon.Data at(int blockX, int blockY, int blockZ, Biome biome) {
        return new DeepDungeon.Data(this, blockX, blockY, blockZ, biome);
    }

    /**
     * Placement data for a {@link DeepDungeon}: the chunk-internal offset and block height.
     */
    public static class Data extends Decorator.Data<DeepDungeon> {

        public final int offsetX;
        public final int offsetZ;
        public final int blockY;

        public Data(DeepDungeon feature, int blockX, int blockY, int blockZ, Biome biome) {
            super(feature, blockX >> 4, blockZ >> 4, biome);
            this.offsetX = blockX & 15;
            this.offsetZ = blockZ & 15;
            this.blockY = blockY;
        }
    }
}
