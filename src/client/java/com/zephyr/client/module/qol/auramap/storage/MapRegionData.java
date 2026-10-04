package com.zephyr.client.module.qol.auramap.storage;

import java.util.Arrays;
import java.util.BitSet;

public final class MapRegionData {
    public final RegionPos pos;
    private final int[] pixels;
    private boolean dirty;
    private final BitSet dirtyTiles = new BitSet(RegionPos.REGION_CHUNK_SIZE * RegionPos.REGION_CHUNK_SIZE);

    public MapRegionData(RegionPos pos) {
        this.pos = pos;
        this.pixels = new int[RegionPos.REGION_PIXEL_SIZE * RegionPos.REGION_PIXEL_SIZE];
        Arrays.fill(pixels, 0xFF000000);
    }

    public MapRegionData(RegionPos pos, int[] pixels) {
        this.pos = pos;
        this.pixels = pixels;
        this.dirty = false;
    }

    public int[] pixels() { return pixels; }
    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; dirtyTiles.clear(); }
    public void clearSaveFlag() { dirty = false; }
    public void clearTextureTiles() { dirtyTiles.clear(); }

    public BitSet peekDirtyTiles() {
        if (dirtyTiles.isEmpty()) return null;
        return (BitSet) dirtyTiles.clone();
    }

    public BitSet consumeDirtyTiles() {
        if (dirtyTiles.isEmpty()) return null;
        BitSet copy = (BitSet) dirtyTiles.clone();
        dirtyTiles.clear();
        return copy;
    }

    public boolean hasDirtyTiles() { return !dirtyTiles.isEmpty(); }

    public void putChunkTile(int chunkX, int chunkZ, int[] tile16) {
        int localChunkX = Math.floorMod(chunkX, RegionPos.REGION_CHUNK_SIZE);
        int localChunkZ = Math.floorMod(chunkZ, RegionPos.REGION_CHUNK_SIZE);
        int basePx = localChunkX * 16;
        int basePz = localChunkZ * 16;
        int stride = RegionPos.REGION_PIXEL_SIZE;
        for (int z = 0; z < 16; z++) {
            int srcOff = z * 16;
            int dstOff = (basePz + z) * stride + basePx;
            System.arraycopy(tile16, srcOff, pixels, dstOff, 16);
        }
        dirty = true;
        int tileIdx = localChunkZ * RegionPos.REGION_CHUNK_SIZE + localChunkX;
        dirtyTiles.set(tileIdx);
    }

    public int getPixel(int blockX, int blockZ) {
        int lx = Math.floorMod(blockX, RegionPos.REGION_BLOCK_SIZE);
        int lz = Math.floorMod(blockZ, RegionPos.REGION_BLOCK_SIZE);
        return pixels[lz * RegionPos.REGION_PIXEL_SIZE + lx];
    }
}
