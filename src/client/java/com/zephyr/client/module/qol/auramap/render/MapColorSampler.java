package com.zephyr.client.module.qol.auramap.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;

public final class MapColorSampler {
    private MapColorSampler() {}

    public record Sample(int rgb, int height, boolean transparent, BlockState state, BlockPos pos) {}

    public static Sample sample(LevelChunk chunk, int localX, int localZ, int topY, int minY) {
        return scan(chunk, localX, localZ, topY, minY);
    }

    public static boolean scanFast(LevelChunk chunk, int localX, int localZ, int topY, int minY,
            BlockPos.MutableBlockPos mutable, FastOut out) {
        int baseX = chunk.getPos().getMinBlockX() + localX;
        int baseZ = chunk.getPos().getMinBlockZ() + localZ;
        for (int y = topY; y >= minY; y--) {
            mutable.set(baseX, y, baseZ);
            BlockState state = chunk.getBlockState(mutable);
            if (state.isAir()) continue;
            int cached = com.zephyr.client.module.qol.auramap.cache.BlockColorCache.get(state, () -> state.getMapColor(chunk.getLevel(), mutable));
            if (com.zephyr.client.module.qol.auramap.cache.BlockColorCache.isTransparent(cached)) continue;
            out.rgb = cached & 0xFFFFFF;
            out.height = y;
            out.state = state;
            return true;
        }
        return false;
    }

    public static final class FastOut {
        public int rgb;
        public int height;
        public BlockState state;
    }

    public static int sectionBasedHeight(LevelChunk chunk, int startY) {
        var sections = chunk.getSections();
        if (sections.length == 0) return chunk.getMinY();
        int chunkBottomY = chunk.getMinY();
        int playerSection = Math.min((startY - chunkBottomY) >> 4, sections.length - 1);
        if (playerSection < 0) playerSection = 0;
        int result = chunkBottomY;
        for (int i = playerSection; i < sections.length; i++) {
            if (sections[i].hasOnlyAir()) continue;
            result = chunkBottomY + (i << 4) + 15;
        }
        if (playerSection > 0 && result == chunkBottomY) {
            for (int i = playerSection - 1; i >= 0; i--) {
                if (sections[i].hasOnlyAir()) continue;
                result = chunkBottomY + (i << 4) + 15;
                break;
            }
        }
        return result;
    }

    public static Sample sampleAround(LevelChunk chunk, int localX, int localZ, int estimatedY, int radius, int minY, int maxY) {
        int low = Math.max(minY, estimatedY - radius);
        int high = Math.min(maxY, estimatedY + radius);
        Sample s = scan(chunk, localX, localZ, high, low);
        if (!s.transparent()) return s;

        if (high < maxY) {
            s = scan(chunk, localX, localZ, maxY, high + 1);
            if (!s.transparent()) return s;
        }
        return s;
    }

    private static Sample scan(LevelChunk chunk, int localX, int localZ, int topY, int minY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = topY; y >= minY; y--) {
            pos.set(chunk.getPos().getMinBlockX() + localX, y, chunk.getPos().getMinBlockZ() + localZ);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir()) continue;
            int cached = com.zephyr.client.module.qol.auramap.cache.BlockColorCache.get(state, () -> state.getMapColor(chunk.getLevel(), pos));
            if (com.zephyr.client.module.qol.auramap.cache.BlockColorCache.isTransparent(cached)) continue;
            int base = cached & 0xFFFFFF;
            BlockPos copy = pos.immutable();
            return new Sample(base | 0xFF000000, y, false, state, copy);
        }
        return new Sample(0xFF000000, minY, true, null, null);
    }
}
