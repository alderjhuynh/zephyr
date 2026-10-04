package com.zephyr.client.module.qol.auramap.world;

import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.ConcurrentHashMap;

public final class ChunkDirtyTracker {
    private ChunkDirtyTracker() {}

    private static final ConcurrentHashMap<Long, Boolean> DIRTY = new ConcurrentHashMap<>();

    public static void markDirty(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                DIRTY.put(ChunkPos.pack(chunkX + dx, chunkZ + dz), Boolean.TRUE);
            }
        }
    }

    public static boolean consumeDirty(int chunkX, int chunkZ) {
        return DIRTY.remove(ChunkPos.pack(chunkX, chunkZ)) != null;
    }

    public static boolean isDirty(int chunkX, int chunkZ) {
        return DIRTY.containsKey(ChunkPos.pack(chunkX, chunkZ));
    }

    public static void clear() {
        DIRTY.clear();
    }
}
