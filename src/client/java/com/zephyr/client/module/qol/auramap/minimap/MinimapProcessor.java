package com.zephyr.client.module.qol.auramap.minimap;

public final class MinimapProcessor {
    private double zoom = 1.0;

    public double updateZoom(double target) {
        double off = target - zoom;
        if (Math.abs(off) < 0.01) {
            zoom = target;
        } else {
            zoom = target - off * 0.8;
        }
        return zoom;
    }

    public double zoom() { return zoom; }
    public void snapZoom(double target) { zoom = target; }

    public static double blocksAcross(int renderDistance) {
        return (double) renderDistance * 16.0;
    }

    public static double radiusBlocks(double blocksAcross, double zoom, boolean rotated) {
        double maxVisible = rotated ? blocksAcross * Math.sqrt(2.0) : blocksAcross;
        return (maxVisible / 2.0) / zoom;
    }

    public record ChunkRange(int minMx, int minMz, int maxMx, int maxMz) {}

    public static ChunkRange visibleRange(int playerBlockX, int playerBlockZ, double radiusBlocks) {
        int xFloored = playerBlockX;
        int zFloored = playerBlockZ;
        int playerMx = Math.floorDiv(xFloored, MinimapChunkKey.BLOCK_SIZE);
        int playerMz = Math.floorDiv(zFloored, MinimapChunkKey.BLOCK_SIZE);
        int offsetX = Math.floorMod(xFloored, MinimapChunkKey.BLOCK_SIZE);
        int offsetZ = Math.floorMod(zFloored, MinimapChunkKey.BLOCK_SIZE);
        int minMx = playerMx + (int) Math.floor((offsetX - radiusBlocks) / MinimapChunkKey.BLOCK_SIZE);
        int minMz = playerMz + (int) Math.floor((offsetZ - radiusBlocks) / MinimapChunkKey.BLOCK_SIZE);
        int maxMx = playerMx + (int) Math.floor((offsetX + 1 + radiusBlocks) / MinimapChunkKey.BLOCK_SIZE);
        int maxMz = playerMz + (int) Math.floor((offsetZ + 1 + radiusBlocks) / MinimapChunkKey.BLOCK_SIZE);
        return new ChunkRange(minMx, minMz, maxMx, maxMz);
    }
}
