package com.zephyr.client.module.qol.auramap.minimap;

import java.util.Arrays;

public final class MinimapChunkData {
    public static final int SIZE = 64;

    private final MinimapChunkKey key;
    private final int[] pixels = new int[SIZE * SIZE];
    private volatile boolean dirty;
    private volatile boolean hasSomething;

    public MinimapChunkData(MinimapChunkKey key) {
        this.key = key;
        Arrays.fill(pixels, 0x00000000);
    }

    public MinimapChunkKey key() { return key; }

    public int[] pixels() { return pixels; }

    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; }
    public boolean hasSomething() { return hasSomething; }

    public synchronized void putChunkTile(int chunkX, int chunkZ, int[] tile16) {
        int localChunkX = Math.floorMod(chunkX, MinimapChunkKey.CHUNKS_PER_MINIMAP_CHUNK);
        int localChunkZ = Math.floorMod(chunkZ, MinimapChunkKey.CHUNKS_PER_MINIMAP_CHUNK);
        int basePx = localChunkX * 16;
        int basePz = localChunkZ * 16;
        boolean something = false;
        for (int z = 0; z < 16; z++) {
            int srcOff = z * 16;
            int dstOff = (basePz + z) * SIZE + basePx;
            System.arraycopy(tile16, srcOff, pixels, dstOff, 16);
            if (!something) {
                for (int x = 0; x < 16; x++) {
                    if ((tile16[srcOff + x] & 0xFFFFFF) != 0) { something = true; break; }
                }
            }
        }
        if (something) hasSomething = true;
        dirty = true;
    }
}
