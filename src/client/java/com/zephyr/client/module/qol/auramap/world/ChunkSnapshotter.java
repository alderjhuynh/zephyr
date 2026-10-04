package com.zephyr.client.module.qol.auramap.world;

import com.zephyr.client.module.qol.auramap.render.MapColorSampler;
import com.zephyr.client.module.qol.auramap.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;

public final class ChunkSnapshotter {
    private ChunkSnapshotter() {}

    public static final int CHUNK_SIZE = 16;

    public record ChunkTile(int chunkX, int chunkZ, int[] pixels, int minHeight, int maxHeight) {}

    private static final ThreadLocal<int[]> RAW_RGB = ThreadLocal.withInitial(() -> new int[CHUNK_SIZE * CHUNK_SIZE]);
    private static final ThreadLocal<int[]> HEIGHTS = ThreadLocal.withInitial(() -> new int[CHUNK_SIZE * CHUNK_SIZE]);
    private static final ThreadLocal<int[]> OUT = ThreadLocal.withInitial(() -> new int[CHUNK_SIZE * CHUNK_SIZE]);
    private static final ThreadLocal<boolean[]> TRANSPARENT = ThreadLocal.withInitial(() -> new boolean[CHUNK_SIZE * CHUNK_SIZE]);
    private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<MapColorSampler.FastOut> FAST_OUT = ThreadLocal.withInitial(MapColorSampler.FastOut::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> NEIGHBOR_MUTABLE =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<MapColorSampler.FastOut> NEIGHBOR_FAST =
            ThreadLocal.withInitial(MapColorSampler.FastOut::new);

    public static ChunkTile snapshot(LevelChunk chunk) {
        return snapshotInternal(chunk, CaveModeTracker.SURFACE, 0);
    }

    public static ChunkTile snapshotCave(LevelChunk chunk, int caveStart, int caveDepth) {
        return snapshotInternal(chunk, caveStart, Math.max(1, caveDepth));
    }

    private static ChunkTile snapshotInternal(LevelChunk chunk, int caveStart, int caveDepth) {
        int cx = chunk.getPos().x();
        int cz = chunk.getPos().z();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY() - 1;

        int[] rawRgb = RAW_RGB.get();
        int[] heights = HEIGHTS.get();
        boolean[] transparent = TRANSPARENT.get();
        var level = chunk.getLevel();
        var mutable = MUTABLE.get();
        var fast = FAST_OUT.get();

        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        boolean cave = caveStart != CaveModeTracker.SURFACE;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                int idx = z * CHUNK_SIZE + x;
                boolean found;
                if (cave) {
                    int top = Math.min(caveStart, maxY);
                    int low = Math.max(minY, caveStart + 1 - caveDepth);
                    found = low <= top && scanCaveFast(chunk, x, z, top, low, mutable, fast);
                } else {
                    int mapped = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    int startY = mapped < minY
                            ? MapColorSampler.sectionBasedHeight(chunk, 64)
                            : Math.min(mapped, maxY);
                    found = MapColorSampler.scanFast(chunk, x, z, startY, minY, mutable, fast);
                }
                if (!found) {
                    transparent[idx] = true;
                    heights[idx] = minY;
                    rawRgb[idx] = 0;
                } else {
                    transparent[idx] = false;
                    rawRgb[idx] = fast.rgb;
                    heights[idx] = fast.height;
                    globalMin = Math.min(globalMin, fast.height);
                    globalMax = Math.max(globalMax, fast.height);
                }
            }
        }
        if (globalMin == Integer.MAX_VALUE) globalMin = minY;
        if (globalMax == Integer.MIN_VALUE) globalMax = minY;

        int[] out = OUT.get();
        var cfg = com.zephyr.client.module.qol.auramap.AuraMapController.CONFIG;
        boolean doBiomeBlend = cfg == null || cfg.biomeBlending;
        boolean doDepth = cfg == null || cfg.terrainDepth;
        boolean doShading = cfg == null || cfg.terrainShading;
        int slopeMode = doShading ? 2 : 0;
        boolean doLighting = cfg == null || cfg.lighting;
        int baseBlockX = chunk.getPos().getMinBlockX();
        int baseBlockZ = chunk.getPos().getMinBlockZ();

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                int idx = z * CHUNK_SIZE + x;
                if (transparent[idx]) {
                    out[idx] = 0xFF000000;
                    continue;
                }
                int base = rawRgb[idx];
                int h = heights[idx];

                int muted = ColorUtil.desaturate(base, 0.18f);

                boolean needsPos = doBiomeBlend || doLighting;
                BlockPos p = null;
                BlockState surfaceState = null;
                if (needsPos) {
                    mutable.set(baseBlockX + x, h, baseBlockZ + z);
                    p = mutable;
                    if (doBiomeBlend) {
                        surfaceState = chunk.getBlockState(p);
                    }
                }

                if (doBiomeBlend && p != null && surfaceState != null) {
                    int kind = tintKind(surfaceState);
                    if (kind != NO_TINT) {
                        var biome = level.getBiome(p).value();
                        int tint;
                        if (kind == GRASS_TINT) {
                            tint = biome.getGrassColor(p.getX(), p.getZ());
                        } else {
                            tint = biome.getFoliageColor();
                        }
                        muted = ColorUtil.biomeTint(muted, tint);
                        muted = ColorUtil.desaturate(muted, 0.08f);
                    }
                }

                if (doLighting && p != null) {
                    int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
                    int blk = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p);
                    float min = 9f;
                    float bright = (min + Math.max(sky, blk)) / (15f + min);
                    bright = Math.max(0.65f, Math.min(1.0f, bright));
                    muted = ColorUtil.brightnessRgb(muted, bright);
                }

                if (doDepth) {
                    float depth;

                    float norm = (h - 40f) / 88f;
                    norm = Math.max(0f, Math.min(1f, norm));
                    if (slopeMode >= 2) {
                        depth = 0.90f + 0.20f * norm;
                    } else if (slopeMode == 1) {
                        depth = 0.85f + 0.30f * norm;
                    } else {
                        depth = 0.92f + 0.16f * norm;
                    }
                    muted = ColorUtil.brightnessRgb(muted, depth);
                }

                if (slopeMode > 0) {
                    int northH;
                    if (z > 0) {
                        northH = heights[idx - CHUNK_SIZE];
                    } else {
                        northH = neighborHeight(level, baseBlockX + x, baseBlockZ - 1,
                                minY, maxY, h, caveStart, caveDepth);
                    }
                    int diagH;
                    if (x > 0 && z > 0) {
                        diagH = heights[idx - CHUNK_SIZE - 1];
                    } else if (z > 0) {
                        diagH = neighborHeight(level, baseBlockX - 1, baseBlockZ + z - 1,
                                minY, maxY, northH, caveStart, caveDepth);
                    } else {
                        diagH = neighborHeight(level, baseBlockX + x - 1, baseBlockZ - 1,
                                minY, maxY, northH, caveStart, caveDepth);
                    }
                    int vSlope = h - northH;
                    int dSlope = h - diagH;

                    vSlope = Math.max(-12, Math.min(12, vSlope));
                    dSlope = Math.max(-12, Math.min(12, dSlope));
                    muted = ColorUtil.applyHillShade(muted, vSlope, dSlope, slopeMode);
                }

                out[idx] = 0xFF000000 | (muted & 0xFFFFFF);
            }
        }

        return new ChunkTile(cx, cz, out.clone(), globalMin, globalMax);
    }

    private static final int NO_TINT = 0;
    private static final int GRASS_TINT = 1;
    private static final int FOLIAGE_TINT = 2;

   private static int tintKind(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) return GRASS_TINT;
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SUGAR_CANE)) return GRASS_TINT;
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)
                || state.is(Blocks.VINE)) return FOLIAGE_TINT;
        return NO_TINT;
    }

    private static int neighborHeight(net.minecraft.world.level.Level level,
            int blockX, int blockZ, int minY, int maxY, int fallback, int caveStart, int caveDepth) {
        try {
            int ncx = Math.floorDiv(blockX, CHUNK_SIZE);
            int ncz = Math.floorDiv(blockZ, CHUNK_SIZE);
            var access = level.getChunkSource().getChunk(ncx, ncz, false);
            if (!(access instanceof LevelChunk nchunk)) return fallback;
            int lx = Math.floorMod(blockX, CHUNK_SIZE);
            int lz = Math.floorMod(blockZ, CHUNK_SIZE);
            var m = NEIGHBOR_MUTABLE.get();
            var f = NEIGHBOR_FAST.get();
            boolean found;
            if (caveStart != CaveModeTracker.SURFACE) {
                int top = Math.min(caveStart, maxY);
                int low = Math.max(minY, caveStart + 1 - caveDepth);
                found = low <= top && scanCaveFast(nchunk, lx, lz, top, low, m, f);
            } else {
                int mapped;
                try {
                    mapped = nchunk.getHeight(
                            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, lx, lz);
                } catch (Exception e) {
                    return fallback;
                }
                int startY = mapped < minY
                        ? MapColorSampler.sectionBasedHeight(nchunk, 64)
                        : Math.min(mapped, maxY);
                found = MapColorSampler.scanFast(nchunk, lx, lz, startY, minY, m, f);
            }
            if (!found) return minY;
            return f.height;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean scanCaveFast(LevelChunk chunk, int localX, int localZ, int topY, int lowY,
            BlockPos.MutableBlockPos mutable, MapColorSampler.FastOut out) {
        int baseX = chunk.getPos().getMinBlockX() + localX;
        int baseZ = chunk.getPos().getMinBlockZ() + localZ;
        boolean underair = false;
        for (int y = topY; y >= lowY; y--) {
            mutable.set(baseX, y, baseZ);
            BlockState state = chunk.getBlockState(mutable);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                underair = true;
                if (state.isAir()) continue;
            }
            if (!underair) continue;
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
}
