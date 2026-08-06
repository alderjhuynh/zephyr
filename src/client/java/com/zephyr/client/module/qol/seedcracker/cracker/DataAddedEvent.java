package com.zephyr.client.module.qol.seedcracker.cracker;

import com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.TimeMachine;

@FunctionalInterface
public interface DataAddedEvent {

    DataAddedEvent POKE_PILLARS = s -> s.getTimeMachine().poke(TimeMachine.Phase.PILLARS);
    DataAddedEvent POKE_STRUCTURES = s -> s.getTimeMachine().poke(TimeMachine.Phase.STRUCTURES);
    DataAddedEvent POKE_LIFTING = s -> s.getTimeMachine().poke(TimeMachine.Phase.LIFTING);
    DataAddedEvent POKE_BIOMES = s -> s.getTimeMachine().poke(TimeMachine.Phase.BIOMES);

    void onDataAdded(DataStorage dataStorage);

}
