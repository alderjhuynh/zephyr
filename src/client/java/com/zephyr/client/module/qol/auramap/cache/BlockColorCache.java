package com.zephyr.client.module.qol.auramap.cache;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class BlockColorCache {
    private static final int CAP = 4096;
    private static final java.util.concurrent.ConcurrentHashMap<BlockState, Integer> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>(CAP);

    private static final int TRANSPARENT_SENTINEL = Integer.MIN_VALUE;

    public static int get(BlockState state, java.util.function.Supplier<MapColor> supplier) {
        Integer cached = CACHE.get(state);
        if (cached != null) return cached;
        MapColor mc = supplier.get();
        int rgb;
        if (mc == MapColor.NONE) rgb = TRANSPARENT_SENTINEL;
        else {
            MapColor resolved = MapColor.byId(mc.id);
            if (resolved == null) resolved = MapColor.STONE;
            rgb = resolved.col & 0xFFFFFF;
        }
        CACHE.put(state, rgb);
        return rgb;
    }

    public static boolean isTransparent(int cachedRgb) { return cachedRgb == TRANSPARENT_SENTINEL; }
}
