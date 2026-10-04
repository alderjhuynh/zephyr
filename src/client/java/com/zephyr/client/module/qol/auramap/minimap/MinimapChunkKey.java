package com.zephyr.client.module.qol.auramap.minimap;

public record MinimapChunkKey(int mx, int mz) {
    public static final int CHUNKS_PER_MINIMAP_CHUNK = 4;
    public static final int BLOCK_SIZE = CHUNKS_PER_MINIMAP_CHUNK * 16;

    public static MinimapChunkKey fromChunk(int chunkX, int chunkZ) {
        return new MinimapChunkKey(
                Math.floorDiv(chunkX, CHUNKS_PER_MINIMAP_CHUNK),
                Math.floorDiv(chunkZ, CHUNKS_PER_MINIMAP_CHUNK));
    }

    public static MinimapChunkKey fromBlock(int blockX, int blockZ) {
        return new MinimapChunkKey(
                Math.floorDiv(blockX, BLOCK_SIZE),
                Math.floorDiv(blockZ, BLOCK_SIZE));
    }

    public int minBlockX() { return mx * BLOCK_SIZE; }
    public int minBlockZ() { return mz * BLOCK_SIZE; }
}
