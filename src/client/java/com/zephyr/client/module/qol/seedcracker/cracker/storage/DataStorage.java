package com.zephyr.client.module.qol.seedcracker.cracker.storage;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.decorator.DesertWell;
import com.seedfinding.mcfeature.decorator.EndGateway;
import com.seedfinding.mcfeature.structure.BuriedTreasure;
import com.seedfinding.mcfeature.structure.OldStructure;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.Structure;
import com.seedfinding.mcfeature.structure.TriangularStructure;
import com.seedfinding.mcfeature.structure.UniformStructure;
import com.zephyr.client.module.qol.seedcracker.cracker.BiomeData;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.cracker.HashedSeedData;
import com.zephyr.client.module.qol.seedcracker.cracker.PillarData;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.Decorator;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.DeepDungeon;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.Dungeon;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.EmeraldOre;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.WarpedFungus;
import com.zephyr.client.module.qol.seedcracker.finder.BlockUpdateQueue;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central store for all seed constraints collected by the finders.
 *
 * <p>Holds base feature data (structures and decorators), biome data, pillar data and hashed seed
 * data. New constraints are scheduled and processed on the {@link TimeMachine} service; the
 * {@link #SEED_DATA_COMPARATOR} orders base data by how many random bits each feature constrains.
 */
public class DataStorage {

    /** Orders base seed data so structures come first and higher-information features first. */
    public static final Comparator<Entry<Feature.Data<?>>> SEED_DATA_COMPARATOR = (s1, s2) -> {
        boolean isStructure1 = s1.data.feature instanceof Structure;
        boolean isStructure2 = s2.data.feature instanceof Structure;

        //Structures always come before decorators.
        if (isStructure1 != isStructure2) {
            return isStructure2 ? 1 : -1;
        }

        if (s1.equals(s2)) {
            return 0;
        }

        double diff = getBits(s2.data.feature, false) - getBits(s1.data.feature, false);
        return diff == 0 ? 1 : (int) Math.signum(diff);
    };
    public ScheduledSet<Entry<Feature.Data<?>>> baseSeedData = new ScheduledSet<>(SEED_DATA_COMPARATOR);
    public HashedSeedData hashedSeedData = null;
    public BlockUpdateQueue blockUpdateQueue = new BlockUpdateQueue();
    public boolean openGui = false;
    protected TimeMachine timeMachine = new TimeMachine(this);
    protected Set<Consumer<DataStorage>> scheduledData = ConcurrentHashMap.newKeySet();
    protected PillarData pillarData = null;
    protected ScheduledSet<Entry<BiomeData>> biomeSeedData = new ScheduledSet<>(null);

    /**
     * Estimates how many random bits a feature's placement constrains, used to gauge search
     * progress and whether enough data has been collected.
     *
     * @param feature the feature to estimate
     * @param decorators18 whether to count 1.18+ decorators (whose bits are currently unmodelled)
     * @return the estimated bit count
     */
    public static double getBits(Feature<?, ?> feature, boolean decorators18) {
        if (feature instanceof UniformStructure<?> s) {
            return Math.log(s.getOffset() * s.getOffset()) / Math.log(2);
        } else if (feature instanceof TriangularStructure<?> s) {
            return Math.log(s.getPeak() * s.getPeak()) / Math.log(2);
        }
        if (!decorators18 && feature instanceof Decorator && feature.getVersion().isNewerThan(MCVersion.v1_17_1))
            return 0;
        if (feature instanceof BuriedTreasure) return Math.log(100) / Math.log(2);
        if (feature instanceof DesertWell) return Math.log(1000 * 16 * 16) / Math.log(2);
        if (feature instanceof Dungeon) return Math.log(256 * 16 * 16 * 0.125D) / Math.log(2);
        if (feature instanceof DeepDungeon) return Math.log(58 * 16 * 16 * 0.25D) / Math.log(2);
        if (feature instanceof EmeraldOre) return Math.log(28 * 16 * 16 * 0.5D) / Math.log(2);
        if (feature instanceof EndGateway) return Math.log(700 * 16 * 16 * 7) / Math.log(2);
        if (feature instanceof WarpedFungus) return 0;

        throw new UnsupportedOperationException("go do implement bits count for " + feature.getName() + " you fool");
    }

    /**
     * Flushes pending data into the time machine's service. When the machine is idle, scheduled
     * consumer callbacks are run off the main thread and the machine is marked as running.
     */
    public void tick() {
        if (!this.timeMachine.isRunning) {
            this.baseSeedData.dump();
            this.biomeSeedData.dump();
            blockUpdateQueue.tick();

            this.timeMachine.isRunning = true;

            TimeMachine.SERVICE.submit(() -> {
                try {
                    this.scheduledData.removeIf(c -> {
                        c.accept(this);
                        return true;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                this.timeMachine.isRunning = false;
            });
        }
    }

    /**
     * Adds a pillar height constraint. Only the first pillar data set is accepted.
     *
     * @param data the pillar data to store
     * @param event the event to fire once stored
     * @return true if this is the first pillar data set and it was stored
     */
    public synchronized boolean addPillarData(PillarData data, DataAddedEvent event) {
        boolean isAdded = this.pillarData == null;

        if (isAdded && data != null) {
            this.pillarData = data;
            this.schedule(event::onDataAdded);
        }

        return isAdded;
    }

    /**
     * Adds a base (structure/decorator) feature placement constraint if it is not already stored.
     *
     * @param data the feature data to store
     * @param event the event to fire once stored
     * @return true if the data was newly added
     */
    public synchronized boolean addBaseData(Feature.Data<?> data, DataAddedEvent event) {
        Entry<Feature.Data<?>> e = new Entry<>(data, event);

        if (this.baseSeedData.contains(e)) {
            return false;
        }

        this.baseSeedData.scheduleAdd(e);
        this.schedule(event::onDataAdded);
        return true;
    }

    /**
     * Adds a biome constraint if it is not already stored.
     *
     * @param data the biome data to store
     * @param event the event to fire once stored
     * @return true if the data was newly added
     */
    public synchronized boolean addBiomeData(BiomeData data, DataAddedEvent event) {
        Entry<BiomeData> e = new Entry<>(data, event);

        if (this.biomeSeedData.contains(e)) {
            return false;
        }

        this.biomeSeedData.scheduleAdd(e);
        this.schedule(event::onDataAdded);
        return true;
    }

    /**
     * Adds a hashed seed constraint, replacing any previously stored one with a different hash.
     *
     * @param data the hashed seed data to store
     * @param event the event to fire once stored
     * @return true if the hashed seed was newly stored
     */
    public synchronized boolean addHashedSeedData(HashedSeedData data, DataAddedEvent event) {
        if (this.hashedSeedData == null || this.hashedSeedData.getHashedSeed() != data.getHashedSeed()) {
            this.hashedSeedData = data;
            this.schedule(event::onDataAdded);
            return true;
        }

        return false;
    }

    /**
     * Queues a consumer to run on the time machine service during the next {@link #tick()}.
     *
     * @param consumer the callback to run
     */
    public void schedule(Consumer<DataStorage> consumer) {
        this.scheduledData.add(consumer);
    }

    /**
     * @return the {@link TimeMachine} driving the seed search
     */
    public TimeMachine getTimeMachine() {
        return this.timeMachine;
    }

    /**
     * @return total bits constrained by all stored base features (excluding pillager outposts)
     */
    public double getBaseBits() {
        double bits = 0.0D;

        for (Entry<Feature.Data<?>> e : this.baseSeedData) {
            if (!(e.data.feature instanceof PillagerOutpost)) {
                bits += getBits(e.data.feature, false);
            }
        }
        return bits;
    }

    /**
     * @return bits constrained by old-style structures and shipwrecks, used for the lifting phase
     */
    public double getLiftingBits() {
        double bits = 0.0D;

        for (Entry<Feature.Data<?>> e : this.baseSeedData) {
            if (e.data.feature instanceof OldStructure structure) {
                bits += Math.log(structure.getOffset() * structure.getOffset()) / Math.log(2);
            } else if (e.data.feature instanceof Shipwreck shipwreck) {
                bits += Math.log(shipwreck.getOffset() * shipwreck.getOffset()) / Math.log(2);
            }
        }
        return bits;
    }

    /**
     * @return bits constrained by all stored decorator features (counting 1.18+ decorators)
     */
    public double getDecoratorBits() {
        double bits = 0.0D;

        for (Entry<Feature.Data<?>> e : this.baseSeedData) {
            if (e.data.feature instanceof Decorator decorator) {
                bits += getBits(decorator, true);
            }
        }
        return bits;
    }

    /**
     * @return the target amount of bits required before the structure seed search is attempted
     */
    public double getWantedBits() {
        return 32.0D;
    }

    /**
     * @return true if fewer than seven biome constraints have been collected
     */
    public boolean notEnoughBiomeData() {
        return this.biomeSeedData.size() < 7;
    }

    /**
     * Resets all stored data and state for a fresh seed search, terminating the current time
     * machine and replacing it with a new one.
     */
    public void clear() {
        this.scheduledData = ConcurrentHashMap.newKeySet();
        this.pillarData = null;
        this.baseSeedData = new ScheduledSet<>(SEED_DATA_COMPARATOR);
        this.biomeSeedData = new ScheduledSet<>(null);
        //this.hashedSeedData = null;
        this.timeMachine.shouldTerminate = true;
        this.timeMachine = new TimeMachine(this);
        this.blockUpdateQueue = new BlockUpdateQueue();
    }

    /**
     * A single stored constraint paired with the event to fire when it is processed.
     *
     * @param <T> the type of stored data
     */
    public static class Entry<T> {
        /** The stored data. */
        public final T data;
        /** The event fired when this entry's data is processed. */
        public final DataAddedEvent event;

        public Entry(T data, DataAddedEvent event) {
            this.data = data;
            this.event = event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry<?> entry)) return false;

            if (this.data instanceof Feature.Data<?> d1 && entry.data instanceof Feature.Data<?> d2) {
                return d1.feature == d2.feature && d1.chunkX == d2.chunkX && d1.chunkZ == d2.chunkZ;
            } else if (this.data instanceof BiomeData && entry.data instanceof BiomeData) {
                return this.data.equals(entry.data);
            }

            return false;
        }

        @Override
        public int hashCode() {
            if (this.data instanceof Feature.Data<?>) {
                return ((Feature.Data<?>) this.data).chunkX * 31 + ((Feature.Data<?>) this.data).chunkZ;
            } else if (this.data instanceof BiomeData) {
                return this.data.hashCode();
            }

            return super.hashCode();
        }
    }

}
