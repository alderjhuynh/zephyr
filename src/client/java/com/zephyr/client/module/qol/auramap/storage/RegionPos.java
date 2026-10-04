package com.zephyr.client.module.qol.auramap.storage;

public record RegionPos(int rx, int rz) {
    public static final int REGION_CHUNK_SIZE = 32;
    public static final int REGION_BLOCK_SIZE = REGION_CHUNK_SIZE * 16;
    public static final int REGION_PIXEL_SIZE = REGION_BLOCK_SIZE;

    public static RegionPos fromChunk(int chunkX, int chunkZ) {
        return new RegionPos(Math.floorDiv(chunkX, REGION_CHUNK_SIZE), Math.floorDiv(chunkZ, REGION_CHUNK_SIZE));
    }

    public static RegionPos fromBlock(int blockX, int blockZ) {
        return new RegionPos(Math.floorDiv(blockX, REGION_BLOCK_SIZE), Math.floorDiv(blockZ, REGION_BLOCK_SIZE));
    }

    public String fileName() {
        return "r." + rx + "." + rz + ".png";
    }
}
