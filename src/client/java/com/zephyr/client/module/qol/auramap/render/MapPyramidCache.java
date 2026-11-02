package com.zephyr.client.module.qol.auramap.render;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.zephyr.client.module.qol.auramap.storage.MapRegionData;
import com.zephyr.client.module.qol.auramap.storage.RegionFileStorage;
import com.zephyr.client.module.qol.auramap.storage.RegionPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Level-of-detail texture pyramid backing the world map when zoomed out far
 * enough that per-region textures would blow up memory and thread counts.
 *
 * <p>Every tile is a 128 x 128 texture. A level {@code k} tile covers
 * {@code 512 << k} blocks per axis on a quadtree: its four quadrant canvases
 * are the level {@code k - 1} tiles below it, downscaled once. Level 0 tiles
 * are single 512-block regions, built by box-filtering the region's pixel data
 * read from region storage.
 *
 * <p>Memory is bounded by {@link #CANVAS_CAP} canvases (64 KB each, LRU) and
 * work spread across a single background thread. Tiles with no mapped data
 * under them are detected from the already-scanned region file index and
 * cheaply deleted instead of being walked, so zooming out over mostly
 * unexplored space is nearly free.
 */
public final class MapPyramidCache {
    public static final int CANVAS_SIZE = 128;
    public static final int MAX_LEVEL = 8;
    /** Levels at or below this build straight from region files (few per tile). */
    private static final int DIRECT_MAX_LEVEL = 3;
    /** Background colour of unexplored map area, matching the screen fallback. */
    private static final int BACKGROUND = 0xFF14181D;
    /** Max simultaneously retained tile canvases (~64 KB each). */
    private static final int CANVAS_CAP = 2048;
    private static final int BUILDS_STARTED_PER_FRAME = 4;

    public record TileKey(int level, int tx, int tz) {}
    private record TextureEntry(DynamicTexture texture, Identifier id, int[] canvas) {}

    private final RegionFileStorage storage;
    private final String namespace;
    private final Map<TileKey, int[]> canvases = new ConcurrentHashMap<>();
    private final Map<TileKey, TextureEntry> textures = new HashMap<>();
    private final Map<TileKey, Long> lastUsed = new ConcurrentHashMap<>();
    private final Set<TileKey> building = ConcurrentHashMap.newKeySet();
    /**
     * Canvases invalidated by region data updates. Unlike fresh builds these
     * keep rendering their (stale) texture until a rate-limited rebuild has
     * replaced the canvas, so a burst of map writes never makes visible tiles
     * flicker to the background fill.
     */
    private final Set<TileKey> stale = ConcurrentHashMap.newKeySet();
    private final Map<TileKey, Long> lastBuildMs = new ConcurrentHashMap<>();
    private static final long REBUILD_COOLDOWN_MS = 1500;
    private long frameId;

    /** Shared background canvases (one per level); multiplied by level means empty-region fill. */
    private static final Map<Integer, int[]> BLANK = new ConcurrentHashMap<>();

    private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "auramap-pyramid");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    public MapPyramidCache(RegionFileStorage storage, String namespace) {
        this.storage = storage;
        this.namespace = namespace;
    }

    /** Smallest tile level whose tile covers at least ~64 screen pixels. */
    public static int levelForZoom(double zoom) {
        if (!(zoom > 0)) return MAX_LEVEL;
        int k = 0;
        while (k < MAX_LEVEL && (double) (RegionPos.REGION_BLOCK_SIZE << k) * zoom < 64.0) k++;
        return k;
    }

    /** Blocks covered per axis by a tile of the given level. */
    public static long tileBlocks(int level) {
        return (long) RegionPos.REGION_BLOCK_SIZE << level;
    }

    /** Frame bookkeeping: uploads and evictions both budget on this. */
    private int uploadsThisFrame;
    private int buildsStartedThisFrame;

    public void beginFrame() {
        uploadsThisFrame = 0;
        buildsStartedThisFrame = 0;
        frameId++;
        // Mark canvases overlapping updated regions stale; they keep rendering
        // their old content and get rate-limited rebuilds below. +1 covers the
        // emergency coarser level the screen's iteration-cap fallback can use.
        Set<RegionPos> updated = storage.drainUpdatedRegions();
        if (!updated.isEmpty() && !canvases.isEmpty()) {
            for (RegionPos rp : updated) {
                for (int level = 0; level <= MAX_LEVEL + 1; level++) {
                    long n = 1L << level;
                    invalidate(new TileKey(level,
                            Math.floorDiv(rp.rx(), (int) n), Math.floorDiv(rp.rz(), (int) n)));
                }
            }
        }
        // Rebuild a couple of stale canvases per frame at most, with a cooldown
        // so a burst of map writes can't starve or loop the pyramid.
        if (!stale.isEmpty()) {
            long now = System.currentTimeMillis();
            int started = 0;
            for (TileKey key : stale) {
                if (started >= 2) break;
                Long lastBuild = lastBuildMs.get(key);
                if (lastBuild != null && now - lastBuild < REBUILD_COOLDOWN_MS) continue;
                if (building.contains(key)) continue;
                stale.remove(key);
                lastBuildMs.put(key, now);
                startBuild(key, frameId);
                started++;
            }
        }
        evictIfNeeded();
    }

    private void invalidate(TileKey key) {
        if (canvases.containsKey(key)) stale.add(key);
    }

    private void closeTexture(TileKey key) {
        TextureEntry e = textures.remove(key);
        if (e == null) return;
        try { Minecraft.getInstance().getTextureManager().release(e.id); } catch (Exception ignored) {}
        try { e.texture.close(); } catch (Exception ignored) {}
    }

    private void evictIfNeeded() {
        if (canvases.size() <= CANVAS_CAP) return;
        // First: drop canvases not used (rendered or created) for a while.
        TileKey[] keys = canvases.keySet().toArray(new TileKey[0]);
        long cutoff = frameId - 120;
        for (TileKey key : keys) {
            if (canvases.size() <= CANVAS_CAP) return;
            if (lastUsed.getOrDefault(key, Long.MIN_VALUE) < cutoff
                    && !building.contains(key)) {
                canvases.remove(key);
                lastUsed.remove(key);
                stale.remove(key);
                try { closeTexture(key); } catch (Exception ignored) {}
            }
        }
        if (canvases.size() > CANVAS_CAP * 2) {
            // Hard fallback (pathological): drop the oldest half by stamp.
            TileKey[] again = canvases.keySet().toArray(new TileKey[0]);
            int target = canvases.size() - CANVAS_CAP;
            long hardCutoff = stampCutoff(again, target);
            for (TileKey key : again) {
                if (lastUsed.getOrDefault(key, Long.MAX_VALUE) <= hardCutoff) {
                    canvases.remove(key);
                    lastUsed.remove(key);
                    stale.remove(key);
                    try { closeTexture(key); } catch (Exception ignored) {}
                }
            }
        }
    }

    private long stampCutoff(TileKey[] keys, int target) {
        long[] stamps = new long[keys.length];
        for (int i = 0; i < keys.length; i++) stamps[i] = lastUsed.getOrDefault(keys[i], Long.MAX_VALUE >> 1);
        java.util.Arrays.sort(stamps);
        return stamps[Math.min(target, stamps.length) - 1];
    }

    /**
     * Called at render time. Returns the texture id for the tile, scheduling a
     * background build if there is none yet; null means "draw background this
     * frame". Build scheduling and texture uploads are both budgeted per frame.
     */
    public Identifier getOrUpload(TileKey key) {
        lastUsed.put(key, frameId);
        int[] canvas = canvases.get(key);
        if (canvas == null) {
            scheduleBuild(key);
            return null;
        }
        TextureEntry e = textures.get(key);
        if (e == null) {
            if (uploadsThisFrame >= 2) return null;
            uploadsThisFrame++;
            DynamicTexture tex = createTexture(canvas);
            Identifier id = AuraMapBridge.id(namespace + "/pyr_" + key.level()
                    + "_" + key.tx() + "_" + key.tz());
            Minecraft.getInstance().getTextureManager().register(id, tex);
            e = new TextureEntry(tex, id, canvas);
            textures.put(key, e);
        } else if (e.canvas() != canvas) {
            // Canvas was rebuilt underneath the texture; re-upload when budgeted.
            if (uploadsThisFrame >= 2) return e.id();
            uploadsThisFrame++;
            uploadInto(e.texture(), canvas);
            textures.put(key, new TextureEntry(e.texture(), e.id(), canvas));
        }
        return e.id();
    }

    private void scheduleBuild(TileKey key) {
        if (building.contains(key)) return;
        if (buildsStartedThisFrame >= BUILDS_STARTED_PER_FRAME) return;
        buildsStartedThisFrame++;
        startBuild(key, frameId);
    }

    private void startBuild(TileKey key, long scheduledStamp) {
        if (!building.add(key)) return;
        BUILDER.execute(() -> {
            try {
                int[] canvas = hasContentUnder(key.level(), key.tx(), key.tz())
                        ? buildTile(key)
                        : blank(key.level());
                canvases.put(key, canvas);
                // Creation stamp keeps just-built (but not yet rendered) canvas
                // alive for a while, while letting eviction reclaim them later.
                lastUsed.putIfAbsent(key, scheduledStamp);
                stale.remove(key);
            } catch (Exception e) {
                AuraMapBridge.LOGGER.warn("pyramid tile build failed {}", key, e);
            } finally {
                building.remove(key);
            }
        });
    }

    /** True when none of the indexed region files fall inside this tile. */
    private boolean hasContentUnder(int level, int tx, int tz) {
        long span = 1L << level;
        long minRx = (long) tx * span;
        long minRz = (long) tz * span;
        long maxRx = minRx + span - 1;
        long maxRz = minRz + span - 1;
        if (level >= DIRECT_MAX_LEVEL) {
            // Grain = DIRECT_MAX_LEVEL tiles are exactly one known-region grid
            // cell; propagate emptiness upward by checking the covered grains.
            long grainSpan = 1L << DIRECT_MAX_LEVEL;
            long gtx0 = minRx >> DIRECT_MAX_LEVEL;
            long gtz0 = minRz >> DIRECT_MAX_LEVEL;
            long gtx1 = maxRx >> DIRECT_MAX_LEVEL;
            long gtz1 = maxRz >> DIRECT_MAX_LEVEL;
            var grains = storage.knownRegionsByGrain();
            for (long gx = gtx0; gx <= gtx1; gx++) {
                for (long gz = gtz0; gz <= gtz1; gz++) {
                    if (grains.contains(pack((int) gx, (int) gz))) return true;
                }
            }
            return false;
        }
        for (long rx = minRx; rx <= maxRx; rx++) {
            for (long rz = minRz; rz <= maxRz; rz++) {
                if (storage.isKnownRegion((int) rx, (int) rz)) return true;
            }
        }
        return false;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private int[] buildTile(TileKey key) {
        int level = key.level();
        if (level <= DIRECT_MAX_LEVEL) return buildDirect(key);
        return buildComposite(key);
    }

    /** Builds a small (<= 8x8 region) tile straight from the region pixel data. */
    private int[] buildDirect(TileKey key) {
        int level = key.level();
        int regions = 1 << level;
        int slot = CANVAS_SIZE >> level;
        int[] canvas = new int[CANVAS_SIZE * CANVAS_SIZE];
        java.util.Arrays.fill(canvas, BACKGROUND);
        long baseRx = (long) key.tx() * regions;
        long baseRz = (long) key.tz() * regions;
        for (long rx = baseRx; rx < baseRx + regions; rx++) {
            for (long rz = baseRz; rz < baseRz + regions; rz++) {
                if (!storage.isKnownRegion((int) rx, (int) rz)) continue;
                MapRegionData data = storage.getOrLoad(new RegionPos((int) rx, (int) rz));
                if (data == null) continue;
                int[] down = BranchMipCache.downscale512(data.pixels(), slot);
                paste(canvas, down, slot,
                        (int) ((rx - baseRx) * slot), (int) ((rz - baseRz) * slot));
            }
        }
        return canvas;
    }

    /** Builds a deep tile from its four children, launching their builds first. */
    private int[] buildComposite(TileKey key) {
        int childLevel = key.level() - 1;
        int[] canvas = new int[CANVAS_SIZE * CANVAS_SIZE];
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                TileKey child = new TileKey(childLevel, (key.tx() << 1) + dx, (key.tz() << 1) + dz);
                int[] childCanvas = ensureCanvas(child);
                if (childCanvas == null) continue;
                int[] half = BranchMipCache.halve(childCanvas, CANVAS_SIZE);
                paste(canvas, half, CANVAS_SIZE / 2, dx * (CANVAS_SIZE / 2), dz * (CANVAS_SIZE / 2));
            }
        }
        return canvas;
    }

    /** Ensures a child canvas exists, building it synchronously on this thread. */
    private int[] ensureCanvas(TileKey key) {
        if (!hasContentUnder(key.level(), key.tx(), key.tz())) return blank(key.level());
        int[] cached = canvases.get(key);
        if (cached != null) return cached;
        int[] built = buildTile(key);
        canvases.put(key, built);
        return built;
    }

    private static int[] blank(int level) {
        return BLANK.computeIfAbsent(level, l -> {
            int[] c = new int[CANVAS_SIZE * CANVAS_SIZE];
            java.util.Arrays.fill(c, BACKGROUND);
            return c;
        });
    }

    private static void paste(int[] dst, int[] src, int srcSize, int dx, int dz) {
        for (int z = 0; z < srcSize; z++) {
            System.arraycopy(src, z * srcSize, dst, (dz + z) * CANVAS_SIZE + dx, srcSize);
        }
    }

    private static DynamicTexture createTexture(int[] canvas) {
        NativeImage img = new NativeImage(CANVAS_SIZE, CANVAS_SIZE, false);
        writeInto(img, canvas);
        return new DynamicTexture(() -> "auramap pyramid tile", img);
    }

    private static void uploadInto(DynamicTexture tex, int[] canvas) {
        try {
            NativeImage img = tex.getPixels();
            if (img == null) return;
            writeInto(img, canvas);
            tex.upload();
        } catch (Exception ex) {
            AuraMapBridge.LOGGER.warn("pyramid tile upload failed", ex);
        }
    }

    private static void writeInto(NativeImage img, int[] canvas) {
        int size = CANVAS_SIZE;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = canvas[y * size + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                img.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    /** Releases every texture; called when the map screen closes. */
    public void close() {
        for (TileKey key : textures.keySet().toArray(new TileKey[0])) {
            try { closeTexture(key); } catch (Exception ignored) {}
        }
        canvases.clear();
        lastUsed.clear();
    }
}
