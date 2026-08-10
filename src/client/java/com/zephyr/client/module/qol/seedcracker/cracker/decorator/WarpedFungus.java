package com.zephyr.client.module.qol.seedcracker.cracker.decorator;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mccore.version.VersionMap;
import com.seedfinding.mcreversal.PopulationReverser;
import com.seedfinding.mcseed.lcg.LCG;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.TimeMachine;
import com.zephyr.client.module.qol.seedcracker.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Warped fungus decorator (Nether).
 *
 * <p>When a sufficiently informative fungus is observed, {@code Data.onDataAdded} cracks its
 * structure back into candidate structure seeds by reversing the population seed, then pokes the
 * {@link TimeMachine}.
 */
public class WarpedFungus extends Decorator<Decorator.Config, WarpedFungus.Data> {
    public static final VersionMap<Config> CONFIGS = new VersionMap<Config>()
            .add(MCVersion.v1_16, new Decorator.Config(8, 3));

    public WarpedFungus(MCVersion version) {
        super(CONFIGS.getAsOf(version), version);
    }

    /**
     * Builds warped fungus placement data from observed block positions.
     *
     * @param blockX the chunk block X
     * @param blockZ the chunk block Z
     * @param biome the biome the fungus was found in
     * @param posList the observed stem positions within the chunk
     * @param fungusData the structural snapshot of the best fungus
     * @return the placement data
     */
    public WarpedFungus.Data at(int blockX, int blockZ, Biome biome, List<BlockPos> posList, FullFungusData fungusData) {
        return new WarpedFungus.Data(this, blockX, blockZ, biome, posList, fungusData);
    }

    @Override
    public String getName() {
        return "warped fungus";
    }

    @Override
    public boolean canStart(WarpedFungus.Data data, long structureSeed, ChunkRand rand) {
        return true;
    }

    @Override
    public boolean canStart(WarpedFungus.Data data, long structureSeed, WorldgenRandom rand) {
        return true;
    }

    @Override
    public boolean isValidDimension(Dimension dimension) {
        return dimension == Dimension.NETHER;
    }

    @Override
    public Dimension getValidDimension() {
        return Dimension.NETHER;
    }

    /**
     * @param biome the biome to validate
     * @return true only for the warped forest
     */
    @Override
    public boolean isValidBiome(Biome biome) {
        return biome == Biomes.WARPED_FOREST;
    }

    /**
     * Placement data for a {@link WarpedFungus}, holding the full fungus structure snapshot plus the
     * observed stem positions.
     */
    public static class Data extends Decorator.Data<WarpedFungus> {

        /** Structural snapshot of the observed fungus. */
        public final FullFungusData fullFungi;
        /** Observed stem positions, normalised to the current chunk. */
        public final List<BlockPos> posList = new ArrayList<>();
        private final int BlockX;
        private final int BlockZ;

        public Data(WarpedFungus feature, int blockX, int blockZ, Biome biome, List<BlockPos> posList, FullFungusData fungusData) {
            super(feature, blockX, blockZ, biome);
            fullFungi = fungusData;
            BlockX = blockX;
            BlockZ = blockZ;
            for (BlockPos pos : posList) {
                this.posList.add(new BlockPos(((pos.getX() % 16) + 16) % 16, 0, ((pos.getZ() % 16) + 16) % 16));
            }
        }

        /**
         * Cracks the observed fungus back to candidate structure seeds and pokes the
         * {@link TimeMachine}. Stems are matched by walking backwards through the population random.
         *
         * @param dataStorage the storage to submit recovered seeds through
         */
        public void onDataAdded(DataStorage dataStorage) {
            if (dataStorage.getTimeMachine().worldSeeds.size() == 1) return;
            if (dataStorage.getTimeMachine().structureSeeds.size() == 1) return;
            if (this.feature.getVersion().isNewerThan(MCVersion.v1_17_1)) return;

            Log.warn("fungus.start", BlockX, BlockZ);

            List<Long> fungusSeeds = fullFungi.crackSeed().boxed().toList();

            Log.debug("====================================");
            fungusSeeds.forEach(s -> Log.printSeed("fungus.fungusSeed", s));

            if (fungusSeeds.isEmpty()) {
                Log.warn("fungus.wrongData");
                Log.debug("====================================");
                return;
            }
            Log.debug("====================================");

            LinkedHashSet<Long> structureSeedList = new LinkedHashSet<>();
            fungusSeeds.forEach(s -> structureSeedList.addAll(reverseToDecoratorSeed(s)));
            if (structureSeedList.isEmpty()) {
                Log.warn("fungus.noStructureSeedsFound");
                return;
            }

            Set<Long> result = new HashSet<>();

            Log.warn("fungus.gotStructureSeeds");
            for (Long structureSeed : structureSeedList) {
                Log.printSeed("foundStructureSeed", structureSeed);
                if (!dataStorage.getTimeMachine().structureSeeds.add(structureSeed)) {
                    result.add(structureSeed);
                }
            }

            if (result.size() == 1) {
                Log.debug("====================================");
                result.forEach(seed -> Log.printSeed("crossCompare", seed));
                Log.warn("fungus.usableAsWorldSeed");
                dataStorage.getTimeMachine().structureSeeds = result;
            }
            dataStorage.getTimeMachine().poke(TimeMachine.Phase.BIOMES);
        }


        private LinkedHashSet<Long> reverseToDecoratorSeed(long fungusSeed) {
            LinkedHashSet<Long> structureSeedList = new LinkedHashSet<>();
            ChunkRand rand = new ChunkRand(fungusSeed, false);
            ChunkRand rand2 = new ChunkRand(0);
            ChunkRand popReverseRand = new ChunkRand(0);
            rand.advance(-4000);
            int lastPos = -1;
            for (int i = 0; i < 4000; i++) {
                int pos = rand.nextInt(16);
                for (BlockPos fungusPos : posList) {
                    if (fungusPos.getX() == lastPos && fungusPos.getZ() == pos) {
                        rand.advance(-2);
                        if (checkPoses(rand.getSeed())) {

                            rand2.setSeed(rand.getSeed(), false);
                            for (int j = 0; j < 8; j++) {
                                long populationSeed = (rand2.getSeed() ^ LCG.JAVA.multiplier) - 80003;
                                structureSeedList.addAll(PopulationReverser.reverse(populationSeed, BlockX, BlockZ, popReverseRand, MCVersion.v1_16_2));
                                rand2.advance(-2);
                            }

                        }
                        rand.advance(2);
                        break;
                    }
                }
                lastPos = pos;
            }
            return structureSeedList;
        }

        private boolean checkPoses(long seed) {
            List<BlockPos> posesClone = new ArrayList<>(posList);
            ChunkRand rand = new ChunkRand(seed, false);
            int counter = 0;
            for (int i = 0; i < 100; i++) {
                counter++;
                if (counter > 10) {
                    return false;
                }
                int x = rand.nextInt(16);
                int z = rand.nextInt(16);
                for (int j = 0; j < posesClone.size(); j++) {
                    if (posesClone.get(j).getX() == x && posesClone.get(j).getZ() == z) {
                        counter = 0;
                        posesClone.remove(j);
                        break;
                    }
                }
                if (posesClone.isEmpty()) return true;
            }
            return false;
        }
    }
}

