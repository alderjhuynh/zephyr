package com.zephyr.client.module.qol.auramap.render;

import com.zephyr.client.module.qol.auramap.storage.RegionPos;

public final class BranchMipCache {
    private BranchMipCache() {}

    public static int[] downscale512to256(int[] src512) {
        int[] dst = new int[256 * 256];
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int sx = x * 2, sy = y * 2;
                int c00 = src512[sy * 512 + sx];
                int c01 = src512[sy * 512 + sx + 1];
                int c10 = src512[(sy + 1) * 512 + sx];
                int c11 = src512[(sy + 1) * 512 + sx + 1];

                int r = (((c00 >> 16 & 0xFF) + (c01 >> 16 & 0xFF) + (c10 >> 16 & 0xFF) + (c11 >> 16 & 0xFF)) / 4);
                int g = (((c00 >> 8 & 0xFF) + (c01 >> 8 & 0xFF) + (c10 >> 8 & 0xFF) + (c11 >> 8 & 0xFF)) / 4);
                int b = (((c00 & 0xFF) + (c01 & 0xFF) + (c10 & 0xFF) + (c11 & 0xFF)) / 4);
                int a = 0xFF;
                dst[y * 256 + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return dst;
    }

    public static RegionPos branchPos(RegionPos leaf, int level) {
        int shift = level;
        return new RegionPos(leaf.rx() >> shift, leaf.rz() >> shift);
    }
}
