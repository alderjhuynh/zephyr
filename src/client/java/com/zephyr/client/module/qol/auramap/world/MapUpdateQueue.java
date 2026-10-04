package com.zephyr.client.module.qol.auramap.world;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.zephyr.client.module.qol.auramap.minimap.MinimapDataStore;
import com.zephyr.client.module.qol.auramap.storage.RegionFileStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MapUpdateQueue {
    private static final int MAX_CHUNKS_PER_DRAIN = 24;
    private static final long MAX_NANOS_PER_DRAIN = 6_000_000L;

    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "auramap-writer");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "auramap-io");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private volatile RegionFileStorage storage;
    private volatile RegionFileStorage caveStorage;
    private volatile MinimapDataStore minimapStore;
    private volatile boolean closed;
    
    public record CaveState(boolean active, int start, int depth) {
        public static final CaveState SURFACE = new CaveState(false, Integer.MAX_VALUE, 30);
    }

    private volatile CaveState caveState = CaveState.SURFACE;
    private boolean lastCaveActive;
    private int lastCaveBand;

    private final Map<Long, long[]> pendingDirty = new ConcurrentHashMap<>();
    private final Map<Long, int[]> lastSurfacePixels = new ConcurrentHashMap<>();
    private final Map<Long, int[]> lastCavePixels = new ConcurrentHashMap<>();
    private static final int LAST_CAP = 1024;

    public void setStorage(RegionFileStorage s) {
        RegionFileStorage old = this.storage;
        if (old != null) {
            final RegionFileStorage toFlush = old;
            io.execute(() -> {
                try { toFlush.flushDirtyForced(); } catch (Exception e) { AuraMapBridge.LOGGER.warn("[auramap] flush failed", e); }
            });
        }
        this.storage = s;
        pendingDirty.clear();
    }

    public void setMinimapStore(MinimapDataStore minimapStore) {
        this.minimapStore = minimapStore;
        pendingDirty.clear();
    }

    public void setCaveStorage(RegionFileStorage s) {
        RegionFileStorage old = this.caveStorage;
        if (old != null && old != s) {
            final RegionFileStorage toFlush = old;
            io.execute(() -> {
                try { toFlush.flushDirtyForced(); } catch (Exception e) { AuraMapBridge.LOGGER.warn("[auramap] flush failed", e); }
            });
        }
        this.caveStorage = s;
        lastCavePixels.clear();
        lastCaveBand = 0;
        lastCaveActive = false;
    }

    public void setCaveState(CaveState caveState) {
        this.caveState = caveState != null ? caveState : CaveState.SURFACE;
    }

    public void markDirty(int chunkX, int chunkZ) {
        if (closed || storage == null) return;
        pendingDirty.put(ChunkPos.pack(chunkX, chunkZ), new long[]{chunkX, chunkZ});
    }

    public void enqueue(LevelChunk chunk) {
        markDirty(chunk.getPos().x(), chunk.getPos().z());
    }

    public void drainWithBudget(net.minecraft.client.multiplayer.ClientLevel level, int playerChunkX, int playerChunkZ, int radius) {
        if (closed || storage == null || level == null) return;
        RegionFileStorage s = storage;

        CaveState cave = caveState;
        boolean useCave = cave.active() && caveStorage != null;
        RegionFileStorage fs = useCave ? caveStorage : s;
        Map<Long, int[]> last = useCave ? lastCavePixels : lastSurfacePixels;

        if (useCave != lastCaveActive) {
            last.clear();
            lastCaveActive = useCave;
        }
        if (useCave) {
            int band = cave.start() >> 4;
            if (band != lastCaveBand) {
                lastCavePixels.clear();
                lastCaveBand = band;
            }
        }

        List<long[]> candidates = new ArrayList<>(pendingDirty.values());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                long key = ChunkPos.pack(cx, cz);
                if (!last.containsKey(key) && !pendingDirty.containsKey(key)) {
                    candidates.add(new long[]{cx, cz});
                }
            }
        }
        if (candidates.isEmpty()) return;

        candidates.sort((a, b) -> {
            int da = Math.max(Math.abs((int) a[0] - playerChunkX), Math.abs((int) a[1] - playerChunkZ));
            int db = Math.max(Math.abs((int) b[0] - playerChunkX), Math.abs((int) b[1] - playerChunkZ));
            return Integer.compare(da, db);
        });

        long start = System.nanoTime();
        int done = 0;
        for (long[] c : candidates) {
            if (done >= MAX_CHUNKS_PER_DRAIN) break;
            if (System.nanoTime() - start >= MAX_NANOS_PER_DRAIN) break;
            int cx = (int) c[0];
            int cz = (int) c[1];
            long key = ChunkPos.pack(cx, cz);
            if (Math.max(Math.abs(cx - playerChunkX), Math.abs(cz - playerChunkZ)) > radius) {
                continue;
            }
            boolean dirty = ChunkDirtyTracker.consumeDirty(cx, cz) || pendingDirty.containsKey(key) || !last.containsKey(key);
            if (!dirty) {
                pendingDirty.remove(key);
                continue;
            }
            var chunk = level.getChunkSource().getChunk(cx, cz, false);
            if (!(chunk instanceof LevelChunk lc)) {
                continue;
            }
            pendingDirty.remove(key);
            final MinimapDataStore mm = minimapStore;
            final boolean caveTile = useCave;
            final int caveStart = cave.start();
            final int caveDepth = cave.depth();
            final Map<Long, int[]> lastForTile = last;
            writer.execute(() -> {
                try {
                    var tile = caveTile
                            ? ChunkSnapshotter.snapshotCave(lc, caveStart, caveDepth)
                            : ChunkSnapshotter.snapshot(lc);
                    int[] prev = lastForTile.get(key);
                    if (prev != null && java.util.Arrays.equals(prev, tile.pixels())) {
                        return;
                    }
                    if (lastForTile.size() > LAST_CAP) lastForTile.clear();
                    lastForTile.put(key, tile.pixels().clone());
                    if (isMostlyBlack(tile.pixels())) {
                        return;
                    }
                    fs.putChunkTile(tile.chunkX(), tile.chunkZ(), tile.pixels());
                    if (mm != null) mm.putChunkTile(tile.chunkX(), tile.chunkZ(), tile.pixels(), caveTile);
                } catch (Exception e) {
                    AuraMapBridge.LOGGER.warn("[auramap] snapshot failed for chunk {},{}", cx, cz, e);
                }
            });
            done++;
        }
    }

    private static boolean isMostlyBlack(int[] pixels) {
        int black = 0;
        for (int p : pixels) if ((p & 0xFFFFFF) == 0) black++;
        return black > 220;
    }

    public void flushAsync() {
        RegionFileStorage s = storage;
        if (s != null) io.execute(() -> {
            try { s.flushDirty(); } catch (Exception e) { AuraMapBridge.LOGGER.warn("[auramap] flush failed", e); }
        });
    }

    public void flushAsyncForced() {
        RegionFileStorage s = storage;
        RegionFileStorage cs = caveStorage;
        if (s != null || cs != null) io.execute(() -> {
            try {
                if (s != null) s.flushDirtyForced();
                if (cs != null && cs != s) cs.flushDirtyForced();
            } catch (Exception e) { AuraMapBridge.LOGGER.warn("[auramap] flush failed", e); }
        });
    }

    public void loadRegionAsync(RegionFileStorage s, com.zephyr.client.module.qol.auramap.storage.RegionPos pos, java.util.function.Consumer<com.zephyr.client.module.qol.auramap.storage.MapRegionData> cb) {
        io.execute(() -> {
            try {
                cb.accept(s.getOrLoad(pos));
            } catch (Exception e) {
                AuraMapBridge.LOGGER.warn("[auramap] async region load failed {}", pos, e);
            }
        });
    }

    public void close() {
        closed = true;
        writer.shutdown();
        io.execute(() -> {
            if (storage != null) try { storage.flushDirtyForced(); } catch (Exception ignored) {}
            if (caveStorage != null && caveStorage != storage) try { caveStorage.flushDirtyForced(); } catch (Exception ignored) {}
        });
        io.shutdown();
    }
}
