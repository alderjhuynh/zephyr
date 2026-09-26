package com.zephyr.client.module.qol.seedcracker.cracker.storage;

import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.rand.seed.PillarSeed;
import com.seedfinding.mccore.rand.seed.StructureSeed;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.structure.OldStructure;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.UniformStructure;
import com.seedfinding.mcseed.lcg.LCG;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.BiomeData;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.Decorator;
import com.zephyr.client.module.qol.seedcracker.util.Log;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Drives the seed recovery search across its distinct phases.
 *
 * <p>Given the constraints stored in {@link DataStorage}, the machine walks the candidate-seed
 * space phase by phase: pillar seeds, structure seeds (with optional lifting of the lower bits),
 * seed reduction and finally world seeds via decorators, hashed seed and biome matching. When the
 * search narrows down to a single world seed, it is pushed to every registered
 * {@link com.zephyr.client.module.qol.seedcracker.api.SeedCrackerAPI} entrypoint.
 */
public class TimeMachine {
    private static final Logger logger = LoggerFactory.getLogger("timeMachine");

    /** Shared executor used for all parallel phases of the search. */
    public static ExecutorService SERVICE = Executors.newFixedThreadPool(5);

    private final LCG inverseLCG = LCG.JAVA.combine(-2);
    /** Whether the machine is currently processing data. */
    public boolean isRunning = false;
    /** Set when the search should be aborted (e.g. world reset). */
    public boolean shouldTerminate = false;
    /** Candidate 16-bit pillar seeds. */
    public List<Integer> pillarSeeds = null;
    /** Candidate structure seeds. */
    public Set<Long> structureSeeds = new HashSet<>();
    /** Candidate world seeds. */
    public Set<Long> worldSeeds = new HashSet<>();
    protected DataStorage dataStorage;

    public TimeMachine(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Kicks off (or continues) the search starting from the given phase, progressing through the
     * phase chain until no further progress can be made. If a single world seed is left, it is
     * pushed to all registered seedcracker API entrypoints.
     *
     * @param phase the phase to start from, or null to skip
     */
    public void poke(Phase phase) {
        if (this.worldSeeds.size() == 1) return;
        this.isRunning = true;

        while (phase != null && !this.shouldTerminate) {
            if (phase != Phase.BIOMES && pokeStructureReduce()) {
                phase = Phase.BIOMES;
                continue;

            } else if (phase == Phase.STRUCTURES) {
                if (!pokeStructures()) break;

            } else if (phase == Phase.LIFTING) {
                if (!pokeStructures() && !pokeLifting()) break;

            } else if (phase == Phase.PILLARS) {
                if (!this.pokePillars()) break;

            } else if (phase == Phase.BIOMES) {
                if (!this.pokeBiomes()) break;
            }

            phase = phase.nextPhase();
        }
        if (this.worldSeeds.size() == 1 && !this.shouldTerminate) {
            long seed = worldSeeds.stream().findFirst().get();
            Seedcracker.entrypoints.forEach(entrypoint -> entrypoint.pushWorldSeed(seed));
        }
    }

    /**
     * Searches the full 16-bit pillar seed space for seeds matching the observed pillar heights.
     *
     * @return true once the pillar seed search has been performed
     */
    protected boolean pokePillars() {
        if (this.pillarSeeds != null || this.dataStorage.pillarData == null) return false;
        this.pillarSeeds = new ArrayList<>();

        Log.debug("====================================");
        Log.warn("tmachine.lookingForPillarSeed");

        for (int pillarSeed = 0; pillarSeed < 1 << 16 && !this.shouldTerminate; pillarSeed++) {
            if (this.dataStorage.pillarData.test(pillarSeed)) {
                Log.printSeed("tmachine.foundPillarSeed", pillarSeed);
                this.pillarSeeds.add(pillarSeed);
            }
        }

        if (!this.pillarSeeds.isEmpty()) {
            Log.warn("tmachine.pillarSeedSearchFinished");
        } else {
            Log.error("finishedSearchNoResult");
        }

        return true;
    }

    /**
     * Lifts the low 19 bits of the structure seed directly from the observed old-style structures
     * and shipwrecks, then filters the resulting seed space against all cached base features.
     *
     * @return true if candidate structure seeds were produced
     */
    protected boolean pokeLifting() {
        if (!this.structureSeeds.isEmpty() || this.dataStorage.getLiftingBits() < 40F) return false;
        List<UniformStructure.Data<?>> dataList = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> e : this.dataStorage.baseSeedData) {
            if (e.data.feature instanceof OldStructure || e.data.feature instanceof Shipwreck) {
                dataList.add((UniformStructure.Data<?>) e.data);
            }
        }
        List<Feature.Data<?>> cache = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
            if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                if (!(entry.data.feature instanceof PillagerOutpost)) {
                    cache.add(entry.data);
                }
            }
        }
        Log.warn("tmachine.startLifting", dataList.size());

        // You could first lift on 1L<<18 with %2 since that would be a smaller range
        // Then lift on 1<<19 with those 1<<18 fixed with % 4 and for nextInt(24)
        // You can even do %8 on 1<<20 (however we included shipwreck so only nextInt(20) so 1<<19 is the max here
        Stream<Long> lowerBitsStream = LongStream.range(0, 1L << 19).boxed().filter(lowerBits -> {
            ChunkRand rand = new ChunkRand();
            for (UniformStructure.Data<?> data : dataList) {
                rand.setRegionSeed(lowerBits, data.regionX, data.regionZ, data.feature.getSalt(), Config.get().getVersion());
                if (rand.nextInt(((UniformStructure<?>)data.feature).getOffset()) % 4 != data.offsetX % 4 ||
                        rand.nextInt(((UniformStructure<?>)data.feature).getOffset()) % 4 != data.offsetZ % 4) {
                    return false;
                }
            }
            return true;
        });

        Stream<Long> seedStream = lowerBitsStream.flatMap(lowerBits ->
                LongStream.range(0, 1L << (48 - 19))
                        .boxed()
                        .map(upperBits -> (upperBits << 19) | lowerBits)
        );

        Stream<Long> strutureSeedStream = seedStream.filter(seed -> {
            ChunkRand rand = new ChunkRand();
            for (Feature.Data<?> data : cache) {
                if (!data.testStart(seed, rand)) {
                    return false;
                }
            }
            return true;
        });

        this.structureSeeds = strutureSeedStream.parallel().collect(Collectors.toSet());

        if (!this.structureSeeds.isEmpty()) {
            Log.warn("tmachine.structureSeedSearchFinished");
        } else {
            Log.error("finishedSearchNoResult");
        }

        return !this.structureSeeds.isEmpty();
    }


    /**
     * Brute-forces structure seeds by combining every pillar seed with the upper world-seed bits,
     * then filtering against the cached base features.
     *
     * @return true once the structure seed search has been performed
     */
    protected boolean pokeStructures() {
        if (this.pillarSeeds == null || !this.structureSeeds.isEmpty() ||
                this.dataStorage.getBaseBits() < this.dataStorage.getWantedBits()) return false;

        List<Feature.Data<?>> cache = new ArrayList<>();

        for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
            if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                if (!(entry.data.feature instanceof PillagerOutpost)) {
                    cache.add(entry.data);
                }
            }
        }

        for (int pillarSeed : this.pillarSeeds) {
            Log.debug("====================================");
            Log.warn("tmachine.lookingForStructureSeeds", pillarSeed);

            AtomicInteger completion = new AtomicInteger();
            ProgressListener progressListener = new ProgressListener();

            for (int threadId = 0; threadId < 4; threadId++) {
                int fThreadId = threadId;

                SERVICE.submit(() -> {
                    ChunkRand rand = new ChunkRand();

                    long lower = (long) fThreadId * (1L << 30);
                    long upper = (long) (fThreadId + 1) * (1L << 30);

                    for (long partialWorldSeed = lower; partialWorldSeed < upper && !this.shouldTerminate; partialWorldSeed++) {
                        if ((partialWorldSeed & ((1 << 27) - 1)) == 0) {
                            progressListener.addPercent(3.125F, true);
                        }

                        long seed = this.timeMachine(partialWorldSeed, pillarSeed);

                        boolean matches = true;

                        for (Feature.Data<?> baseSeedDatum : cache) {
                            if (!baseSeedDatum.testStart(seed, rand)) {
                                matches = false;
                                break;
                            }
                        }

                        if (matches) {
                            this.structureSeeds.add(seed);
                            Log.printSeed("foundStructureSeed", seed);
                        }

                    }

                    completion.getAndIncrement();
                });
            }

            while (completion.get() != 4) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (this.shouldTerminate) {
                    return false;
                }
            }

            progressListener.addPercent(0.0F, true);
        }

        if (!this.structureSeeds.isEmpty()) {
            Log.warn("tmachine.structureSeedSearchFinished");
        } else {
            Log.error("finishedSearchNoResult");
        }

        return true;
    }

    /**
     * Reduces the structure seed candidates down to world seeds, trying (in order): decorator
     * matching for 1.18+, the hashed world seed, a biome-based fuzzy search, a deep biome search
     * and finally a random-seed fallback.
     *
     * @return true if the world seed search produced results
     */
    protected boolean pokeBiomes() {
        if (this.structureSeeds.isEmpty() || this.worldSeeds.size() == 1) return false;
        if (this.structureSeeds.size() > 1000) return false;

        Log.debug("====================================");

        worldSeeds.clear();
        dataStorage.baseSeedData.dump();
        if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_18) && dataStorage.getDecoratorBits() > 32F) {
            Log.warn("tmachine.decoratorWorldSeedSearch");
            WorldgenRandom rand = new WorldgenRandom(new XoroshiroRandomSource(0));


            for (long structureSeed : this.structureSeeds) {
                for (long upperBits = 0; upperBits < 1 << 16 && !this.shouldTerminate; upperBits++) {
                    long worldSeed = (upperBits << 48) | structureSeed;

                    boolean matches = true;

                    for (DataStorage.Entry<Feature.Data<?>> e : this.dataStorage.baseSeedData.getBaseSet()) {
                        if (e.data.feature instanceof Decorator && !((Decorator.Data<?>) e.data).testStart(worldSeed, rand)) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        this.worldSeeds.add(worldSeed);
                        if (this.worldSeeds.size() < 10) {
                            Log.printSeed("tmachine.foundWorldSeed", worldSeed);
                            if (this.worldSeeds.size() == 9) {
                                Log.warn("tmachine.printSeedsInConsole");
                            }
                        } else {
                            logger.info("Found world seed " + worldSeed);
                        }
                    }

                    if (this.shouldTerminate) {
                        return false;
                    }
                }
            }
            if (!this.worldSeeds.isEmpty()) {
                Log.warn("tmachine.worldSeedSearchFinished");
                return true;
            } else {
                Log.warn("finishedSearchNoResult");
            }

        }

        if (this.dataStorage.hashedSeedData != null && this.dataStorage.hashedSeedData.getHashedSeed() != 0) {
            Log.warn("tmachine.hashedSeedWorldSeedSearch");
            for (long structureSeed : this.structureSeeds) {
                WorldSeed.fromHash(structureSeed, this.dataStorage.hashedSeedData.getHashedSeed()).forEach(worldSeed -> {
                    this.worldSeeds.add(worldSeed);
                    Log.printSeed("tmachine.foundWorldSeed", worldSeed);
                });

                if (this.shouldTerminate) {
                    return false;
                }
            }

            if (!this.worldSeeds.isEmpty()) {
                Log.warn("tmachine.worldSeedSearchFinished");
                return true;
            } else {
                this.dataStorage.hashedSeedData = null;
                Log.error("tmachine.noResultsRevertingToBiomes");
            }
        }

        this.dataStorage.biomeSeedData.dump();
        if (this.dataStorage.notEnoughBiomeData()) {
            Log.error("tmachine.moreBiomesNeeded");
            return false;
        }

        Log.warn("tmachine.biomeWorldSeedSearch", this.dataStorage.biomeSeedData.size());
        Log.warn("tmachine.fuzzyBiomeSearch");
        MCVersion version = Config.get().getVersion();
        for (long structureSeed : this.structureSeeds) {
            for (long worldSeed : StructureSeed.toRandomWorldSeeds(structureSeed)) {
                OverworldBiomeSource source = new OverworldBiomeSource(version, worldSeed);

                boolean matches = true;

                for (DataStorage.Entry<BiomeData> e : this.dataStorage.biomeSeedData) {
                    if (!e.data.test(source)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    this.worldSeeds.add(worldSeed);
                    Log.printSeed("tmachine.foundWorldSeed", worldSeed);
                }
                if (this.shouldTerminate) {
                    return false;
                }
            }
        }

        if (!this.worldSeeds.isEmpty()) return true;
        if (this.structureSeeds.size() > 10) return false;
        Log.warn("tmachine.deepBiomeSearch");
        for (long structureSeed : this.structureSeeds) {
            for (long upperBits = 0; upperBits < 1 << 16 && !this.shouldTerminate; upperBits++) {
                long worldSeed = (upperBits << 48) | structureSeed;

                OverworldBiomeSource source = new OverworldBiomeSource(version, worldSeed);

                boolean matches = true;

                for (DataStorage.Entry<BiomeData> e : this.dataStorage.biomeSeedData) {
                    if (!e.data.test(source)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    this.worldSeeds.add(worldSeed);
                    if (this.worldSeeds.size() < 10) {
                        Log.printSeed("tmachine.foundWorldSeed", worldSeed);
                        if (this.worldSeeds.size() == 9) {
                            Log.warn("tmachine.printSeedsInConsole");
                        }
                    } else {
                        logger.info("Found world seed " + worldSeed);
                    }
                }

                if (this.shouldTerminate) {
                    return false;
                }
            }
        }

        dispSearchEnd();

        if (!this.worldSeeds.isEmpty()) return true;

        Log.error("tmachine.deleteBiomeInformation");
        this.dataStorage.biomeSeedData.getBaseSet().clear();

        Log.warn("tmachine.randomSeedSearch");
        for (long structureSeed : this.structureSeeds) {
            StructureSeed.toRandomWorldSeeds(structureSeed).forEach(s ->
                    Log.printSeed("tmachine.foundWorldSeed", s));

        }

        return true;
    }

    /**
     * Attempts to shrink the current structure seed set by re-testing each candidate against the
     * cached base features (and the pillar seeds when known).
     *
     * @return true if the candidate set was successfully reduced
     */
    protected boolean pokeStructureReduce() {
        if (shouldTerminate) return false;
        if (!this.worldSeeds.isEmpty() || this.structureSeeds.size() < 2) return false;
        if (Config.get().getVersion().isOlderThan(MCVersion.v1_13)) return false;

        Set<Long> result = new HashSet<>();
        Log.debug("====================================");
        Log.warn("tmachine.reduceSeeds", this.structureSeeds.size());

        if (this.pillarSeeds != null) {
            structureSeeds.forEach(seed -> {
                if (this.pillarSeeds.contains((int) PillarSeed.fromStructureSeed(seed))) {
                    result.add(seed);
                }
            });
        }

        if (result.size() != 1) {
            this.dataStorage.baseSeedData.dump();
            this.dataStorage.baseSeedData.getBaseSet().removeIf(dataEntry -> !dataEntry.data.feature.getVersion().equals(Config.get().getVersion()));
            List<Feature.Data<?>> cache = new ArrayList<>();

            for (DataStorage.Entry<Feature.Data<?>> entry : this.dataStorage.baseSeedData) {
                if (!(entry.data.feature instanceof Decorator) || entry.data.feature.getVersion().isOlderThan(MCVersion.v1_18)) {
                    //todo remove this when libs are updated
                    if (!(entry.data.feature instanceof PillagerOutpost)) {
                        cache.add(entry.data);
                    }
                }
            }
            ChunkRand rand = new ChunkRand();

            for (Long seed : this.structureSeeds) {
                boolean matches = true;

                for (Feature.Data<?> baseSeedDatum : cache) {
                    if (!baseSeedDatum.testStart(seed, rand)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    result.add(seed);
                }
            }
        }

        if (!result.isEmpty() && this.structureSeeds.size() > result.size()) {
            if (result.size() < 10) {
                result.forEach(seed -> Log.printSeed("foundStructureSeed", seed));
            } else {
                Log.warn("tmachine.succeedReducing", result.size());
            }

            this.structureSeeds = result;
            return true;
        } else {
            Log.warn("tmachine.failedReducing");
        }
        return false;
    }

    private void dispSearchEnd() {
        if (!this.worldSeeds.isEmpty()) {
            Log.warn("tmachine.worldSeedSearchFinished");
        } else {
            Log.error("finishedSearchNoResult");
        }
    }

    /**
     * Reconstructs a structure seed from a partial (upper bits) world seed and a pillar seed by
     * inverting the world-seed transformation.
     *
     * @param partialWorldSeed the upper 30 bits of the world seed
     * @param pillarSeed the 16-bit pillar seed
     * @return the corresponding structure seed
     */
    public long timeMachine(long partialWorldSeed, int pillarSeed) {
        long currentSeed = 0L;
        currentSeed |= (partialWorldSeed & 0xFFFF0000L) << 16;
        currentSeed |= (long) pillarSeed << 16;
        currentSeed |= partialWorldSeed & 0xFFFFL;

        currentSeed = this.inverseLCG.nextSeed(currentSeed);
        currentSeed ^= LCG.JAVA.multiplier;
        return currentSeed;
    }

    /**
     * The distinct phases of the seed search, chained in the order they are visited.
     */
    public enum Phase {
        BIOMES(null), STRUCURE_REDUCE(BIOMES), STRUCTURES(BIOMES), LIFTING(STRUCURE_REDUCE), PILLARS(STRUCTURES);

        private final Phase nextPhase;

        Phase(Phase nextPhase) {
            this.nextPhase = nextPhase;
        }

        /**
         * @return the phase to visit after this one, or null if this is the terminal phase
         */
        public Phase nextPhase() {
            return this.nextPhase;
        }
    }

}
