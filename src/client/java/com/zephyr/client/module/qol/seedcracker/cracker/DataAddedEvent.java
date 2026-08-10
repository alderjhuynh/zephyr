package com.zephyr.client.module.qol.seedcracker.cracker;

import com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.TimeMachine;

/**
 * Callback fired by {@link DataStorage} whenever new seed constraints are recorded.
 *
 * <p>The provided constants poke the {@link TimeMachine} into its next reduction phase so that
 * freshly added data triggers the appropriate candidate-seed search.
 */
@FunctionalInterface
public interface DataAddedEvent {

    /** Poke the {@link TimeMachine.Phase#PILLARS} phase. */
    DataAddedEvent POKE_PILLARS = s -> s.getTimeMachine().poke(TimeMachine.Phase.PILLARS);
    /** Poke the {@link TimeMachine.Phase#STRUCTURES} phase. */
    DataAddedEvent POKE_STRUCTURES = s -> s.getTimeMachine().poke(TimeMachine.Phase.STRUCTURES);
    /** Poke the {@link TimeMachine.Phase#LIFTING} phase. */
    DataAddedEvent POKE_LIFTING = s -> s.getTimeMachine().poke(TimeMachine.Phase.LIFTING);
    /** Poke the {@link TimeMachine.Phase#BIOMES} phase. */
    DataAddedEvent POKE_BIOMES = s -> s.getTimeMachine().poke(TimeMachine.Phase.BIOMES);

    /**
     * Invoked after new data has been added to the storage.
     *
     * @param dataStorage the storage that received the new data
     */
    void onDataAdded(DataStorage dataStorage);

}
