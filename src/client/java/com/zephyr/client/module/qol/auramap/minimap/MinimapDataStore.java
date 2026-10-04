package com.zephyr.client.module.qol.auramap.minimap;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public final class MinimapDataStore {
    private final ConcurrentHashMap<MinimapChunkKey, MinimapChunkData> surface = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MinimapChunkKey, MinimapChunkData> cave = new ConcurrentHashMap<>();
   private volatile boolean caveActive;
    private static final int MAX_CHUNKS = 256;

    public void setCaveActive(boolean caveActive) {
        this.caveActive = caveActive;
    }

    public boolean isCaveActive() {
        return caveActive;
    }

    private ConcurrentHashMap<MinimapChunkKey, MinimapChunkData> active() {
        return caveActive ? cave : surface;
    }

    public MinimapChunkData getIfPresent(MinimapChunkKey key) {
        return active().get(key);
    }

    public MinimapChunkData getOrCreate(MinimapChunkKey key) {
        var chunks = active();
        MinimapChunkData existing = chunks.get(key);
        if (existing != null) return existing;
        MinimapChunkData fresh = new MinimapChunkData(key);
        MinimapChunkData prev = chunks.putIfAbsent(key, fresh);
        if (chunks.size() > MAX_CHUNKS) evictFar(chunks);
        return prev != null ? prev : fresh;
    }

    public void putChunkTile(int chunkX, int chunkZ, int[] tile16) {
        putChunkTile(chunkX, chunkZ, tile16, caveActive);
    }

    public void putChunkTile(int chunkX, int chunkZ, int[] tile16, boolean caveLayer) {
        var chunks = caveLayer ? cave : surface;
        MinimapChunkKey key = MinimapChunkKey.fromChunk(chunkX, chunkZ);
        MinimapChunkData existing = chunks.get(key);
        if (existing == null) {
            MinimapChunkData fresh = new MinimapChunkData(key);
            MinimapChunkData prev = chunks.putIfAbsent(key, fresh);
            existing = prev != null ? prev : fresh;
            if (chunks.size() > MAX_CHUNKS) evictFar(chunks);
        }
        existing.putChunkTile(chunkX, chunkZ, tile16);
    }

    public Collection<MinimapChunkData> all() {
        return active().values();
    }

    public void clear() {
        surface.clear();
        cave.clear();
    }

    private static void evictFar(ConcurrentHashMap<MinimapChunkKey, MinimapChunkData> chunks) {
        int removed = 0;
        for (var e : chunks.entrySet()) {
            if (removed >= 32) break;
            if (!e.getValue().isDirty()) {
                chunks.remove(e.getKey(), e.getValue());
                removed++;
            }
        }
    }

    public void pruneFar(int centerMx, int centerMz, int radiusChunks) {
        pruneFar(surface, centerMx, centerMz, radiusChunks);
        pruneFar(cave, centerMx, centerMz, radiusChunks);
    }

    private static void pruneFar(ConcurrentHashMap<MinimapChunkKey, MinimapChunkData> chunks,
            int centerMx, int centerMz, int radiusChunks) {
        for (var e : chunks.entrySet()) {
            MinimapChunkKey k = e.getKey();
            int dx = Math.abs(k.mx() - centerMx);
            int dz = Math.abs(k.mz() - centerMz);
            if (Math.max(dx, dz) > radiusChunks && !e.getValue().isDirty()) {
                chunks.remove(k, e.getValue());
            }
        }
    }
}
