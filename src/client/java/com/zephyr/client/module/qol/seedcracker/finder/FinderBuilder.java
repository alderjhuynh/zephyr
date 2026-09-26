package com.zephyr.client.module.qol.seedcracker.finder;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Factory used to construct the {@link Finder}s for a given chunk.
 */
@FunctionalInterface
public interface FinderBuilder {

    /**
     * Builds the finder instances to run against a chunk.
     *
     * @param world the level to scan
     * @param chunkPos the chunk to scan
     * @return the finders to execute
     */
    List<Finder> build(Level world, ChunkPos chunkPos);

}
