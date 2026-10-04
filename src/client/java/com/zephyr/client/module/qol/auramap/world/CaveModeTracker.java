package com.zephyr.client.module.qol.auramap.world;

import com.zephyr.client.module.qol.auramap.cache.BlockColorCache;
import com.zephyr.client.module.qol.auramap.config.AuraMapConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveModeTracker {
    public static final int SURFACE = Integer.MAX_VALUE;

    private static final int ROOF_RADIUS = 1;

    private int current = SURFACE;
    private long lastToggleMs = 0;

    public int current() {
        return current;
    }

    public boolean isCave() {
        return current != SURFACE;
    }

    public void reset() {
        current = SURFACE;
        lastToggleMs = 0;
    }

    public boolean tick(ClientLevel level, double playerX, double playerY, double playerZ, AuraMapConfig cfg) {
        int desired = cfg != null && cfg.caveModeAllowed
                ? computeCaveStart(level, playerX, playerY, playerZ)
                : SURFACE;
        boolean wasCave = current != SURFACE;
        boolean wantCave = desired != SURFACE;
        if (wasCave != wantCave) {
            long now = System.currentTimeMillis();
            long waitMs = cfg != null ? (long) (cfg.caveToggleSeconds * 1000f) : 1000L;
            if (now - lastToggleMs > waitMs) {
                lastToggleMs = now;
                current = desired;
                return true;
            }
            return false;
        }
        current = desired;
        return false;
    }

    public static int computeCaveStart(ClientLevel level, double playerX, double playerY, double playerZ) {
        int bottomY = level.getMinY();
        int topLimit = level.getMaxY() - 1;
        int y = (int) Math.floor(playerY) + 1;
        if (y > topLimit || y < bottomY) {
            return SURFACE;
        }
        int defaultCaveStart = y + 3;
        int potentialResult = defaultCaveStart;

        int x = (int) Math.floor(playerX);
        int z = (int) Math.floor(playerZ);
        boolean hasSkyLight = level.dimensionType().hasSkyLight();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int ox = -ROOF_RADIUS; ox <= ROOF_RADIUS; ox++) {
            for (int oz = -ROOF_RADIUS; oz <= ROOF_RADIUS; oz++) {
                int cx = x + ox;
                int cz = z + oz;
                var chunkAccess = level.getChunkSource().getChunk(
                        cx >> 4, cz >> 4, false);
                if (!(chunkAccess instanceof LevelChunk chunk)) {
                    return SURFACE;
                }
                if (hasSkyLight) {
                    mutable.set(cx, y, cz);
                    if (level.getBrightness(LightLayer.SKY, mutable) >= 15) {
                        return SURFACE;
                    }
                }
                int insideX = cx & 15;
                int insideZ = cz & 15;
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, insideX, insideZ);
                if (top < bottomY) {
                    return SURFACE;
                }
                if (top > topLimit) {
                    top = topLimit;
                }
                boolean roofed = false;
                for (int i = y; i <= top; i++) {
                    mutable.set(cx, i, cz);
                    BlockState state = chunk.getBlockState(mutable);
                    if (isRoofBlock(level, state, mutable)) {
                        if (ox == 0 && oz == 0) {
                            potentialResult = Math.min(i, defaultCaveStart);
                        }
                        roofed = true;
                        break;
                    }
                }
                if (!roofed) {
                    return SURFACE;
                }
            }
        }
        return potentialResult;
    }

    private static boolean isRoofBlock(ClientLevel level, BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.typeHolder().is(BlockTags.LEAVES)) {
            return false;
        }
        int cached = BlockColorCache.get(state, () -> state.getMapColor(level, pos));
        if (BlockColorCache.isTransparent(cached)) {
            return false;
        }
        return state.isSolidRender();
    }
}
