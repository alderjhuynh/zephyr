package com.zephyr.client.module.qol.xray.seed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

/**
 * The slice of {@link GenView} that {@link OrePredictor} actually needs.
 *
 * <p>Extends {@link WorldGenLevel} (blocks, chunks, build height, registry access)
 * and adds the seed/write-zone helpers. {@link GenView} implements it over the live
 * {@code ClientLevel}; the parity test harness implements it over a synthetic
 * uniform-stone world, so the exact same prediction code is exercised on both sides.
 */
public interface GenAccess extends WorldGenLevel {

    /** Replicates {@code WorldgenRandom#setDecorationSeed} for a chunk origin. */
    long decorationSeed(int chunkMinX, int chunkMinZ);

    /** Mirrors the vanilla decoration-stage writable zone: write radius 1. */
    boolean canWriteHere(BlockPos pos, int chunkX, int chunkZ);

    /** Mirrors vanilla decoration write rejection directly on coordinates. */
    boolean canWriteHere(int x, int y, int z, int chunkX, int chunkZ);
}
