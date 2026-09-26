package com.zephyr.client.module.qol.seedcracker.cracker.decorator;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mccore.version.VersionMap;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * Emerald ore decorator.
 *
 * <p>Models the emerald ore vein placement roll: between 3 and 25 candidate positions are rolled
 * per chunk (the bound depends on version), and a match is confirmed when one of them equals the
 * observed block offset.
 */
public class EmeraldOre extends Decorator<Decorator.Config, EmeraldOre.Data> {

    public static final VersionMap<Config> CONFIGS = new VersionMap<Config>()
            .add(MCVersion.v1_13, new Decorator.Config(4, 14))
            .add(MCVersion.v1_16, new Decorator.Config(6, 14))
            .add(MCVersion.v1_17, new Decorator.Config(6, 17));

    public EmeraldOre(MCVersion version) {
        super(CONFIGS.getAsOf(version), version);
    }

    @Override
    public String getName() {
        return "emerald_ore";
    }

    /**
     * Rolls the candidate ore positions in the chunk and returns whether one matches the observed
     * block offset.
     *
     * @param data the observed emerald ore placement
     * @param structureSeed the structure/world seed
     * @param rand the seedfinding random already seeded on the decorator seed
     * @return true if a rolled position matches the observed one
     */
    @Override
    public boolean canStart(Data data, long structureSeed, ChunkRand rand) {
        if (this.getVersion().isNewerThan(MCVersion.v1_17_1)) return true;
        super.canStart(data, structureSeed, rand);

        int bound = this.getVersion() == MCVersion.v1_17 ? rand.nextInt(19) + 6 : rand.nextInt(6) + 3;

        for (int i = 0; i < bound; i++) {
            int x, y, z;

            if (this.getVersion().isOlderThan(MCVersion.v1_15)) {
                x = rand.nextInt(16);
                y = rand.nextInt(28) + 4;
                z = rand.nextInt(16);
            } else {
                x = rand.nextInt(16);
                z = rand.nextInt(16);
                y = rand.nextInt(28) + 4;
            }

            if (y == data.blockY && x == data.offsetX && z == data.offsetZ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Vanilla path: emerald ore placement is currently unconstrained for recent versions, so this
     * always accepts.
     *
     * @param data the observed placement
     * @param structureSeed the candidate world seed
     * @param rand the vanilla random
     * @return always true
     */
    @Override
    public boolean canStart(EmeraldOre.Data data, long structureSeed, WorldgenRandom rand) {
        return true;
        //super.canStart(data, structureSeed, rand);
        //int x, y, z;
//
        //for (int i = 0; i < 10; i++) {
//
        //    x = rand.nextInt(16);
        //    z = rand.nextInt(16);
        //    y = rand.nextInt();
//
        //    if (y == data.blockY && x == data.offsetX && z == data.offsetZ) {
        //        return true;
        //    }
//
        //    rand.nextInt(2);
        //    rand.nextInt(2);
        //}
        //return false;
    }

    @Override
    public boolean isValidDimension(Dimension dimension) {
        return dimension == Dimension.OVERWORLD;
    }

    /**
     * @param biome the biome to validate
     * @return true for mountain-type biomes where emerald ore generates
     */
    @Override
    public boolean isValidBiome(Biome biome) {
        return biome == Biomes.GRAVELLY_MOUNTAINS || biome == Biomes.MODIFIED_GRAVELLY_MOUNTAINS
                || biome == Biomes.MOUNTAINS || biome == Biomes.WOODED_MOUNTAINS || biome == Biomes.MOUNTAIN_EDGE;
    }

    /**
     * @return the dimension this decorator generates in
     */
    @Override
    public Dimension getValidDimension() {
        return Dimension.OVERWORLD;
    }


    /**
     * Builds emerald ore placement data from a block position.
     *
     * @param blockX the observed block X
     * @param blockY the observed block Y
     * @param blockZ the observed block Z
     * @param biome the biome the ore was found in
     * @return the placement data
     */
    public EmeraldOre.Data at(int blockX, int blockY, int blockZ, Biome biome) {
        return new EmeraldOre.Data(this, blockX, blockY, blockZ, biome);
    }

    /**
     * Placement data for an {@link EmeraldOre}: the chunk-internal offset and block height.
     */
    public static class Data extends Decorator.Data<EmeraldOre> {
        public final int offsetX;
        public final int blockY;
        public final int offsetZ;

        public Data(EmeraldOre feature, int blockX, int blockY, int blockZ, Biome biome) {
            super(feature, blockX >> 4, blockZ >> 4, biome);
            this.offsetX = blockX & 15;
            this.blockY = blockY;
            this.offsetZ = blockZ & 15;
        }
    }

}
