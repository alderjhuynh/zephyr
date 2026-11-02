package com.zephyr.client.module.qol.auramap.storage;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionFileStorage {
    private static final String ROOT_DIR_NAME = "auramap";
    private static final int MAGIC = 0x4155524D;
    private static final byte VERSION = 1;
    private static final int REGION_SIZE = RegionPos.REGION_PIXEL_SIZE;
    private static final int PIXEL_COUNT = REGION_SIZE * REGION_SIZE;

    private final Path dimRoot;
    private final boolean cave;
    private final Map<RegionPos, MapRegionData> cache = new ConcurrentHashMap<>();
    private final Map<RegionPos, Long> lastAccess = new ConcurrentHashMap<>();
    private static final int MAX_CACHED_REGIONS = 256;

    /**
     * Positions of region files that exist on disk (plus anything written this
     * session). Lets consumers cheaply know where mapped content lives without
     * probing the file system. Populated by a one-time directory scan.
     */
    private final Set<RegionPos> knownRegions = ConcurrentHashMap.newKeySet();
    /** Coarse "which level-3 tile grid cells contain any mapped region" index. */
    private final Set<Long> knownGrains = ConcurrentHashMap.newKeySet();
    /** Regions whose pixel data changed since the last drain. */
    private final Set<RegionPos> updatedRegions = ConcurrentHashMap.newKeySet();

    public RegionFileStorage(String worldId, String dimId) {
        this(worldId, dimId, false);
    }

    public RegionFileStorage(String worldId, String dimId, boolean cave) {
        this.cave = cave;
        Path gameDir = FabricLoader.getInstance().getGameDir();
        String dirName = cave ? dimId + "__cave" : dimId;
        Path root = gameDir.resolve("aura").resolve(ROOT_DIR_NAME).resolve(sanitize(worldId)).resolve(sanitize(dirName));
        this.dimRoot = root;
        try { Files.createDirectories(root); } catch (IOException e) { throw new UncheckedIOException(e); }
        scanKnownRegions();
    }

    /**
     * Scans the region folder once so code that commonly touches thousands of
     * candidate regions (zoomed-out map views) can consult a set instead of
     * hitting the disk for files that do not exist.
     */
    private void scanKnownRegions() {
        try (var stream = Files.list(dimRoot)) {
            var pattern = java.util.regex.Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.(?:bin|png)");
            for (Path file : (Iterable<Path>) stream::iterator) {
                var m = pattern.matcher(file.getFileName().toString());
                if (!m.matches()) continue;
                addKnownRegion(new RegionPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
            }
        } catch (IOException e) {
            AuraMapBridge.LOGGER.warn("failed to index region files in {}", dimRoot, e);
        }
    }

    private void addKnownRegion(RegionPos rp) {
        knownRegions.add(rp);
        knownGrains.add(packGrain(Math.floorDiv(rp.rx(), REGION_GRAIN_REGIONS),
                Math.floorDiv(rp.rz(), REGION_GRAIN_REGIONS)));
    }

    private static final int REGION_GRAIN_REGIONS = 8;

    private static long packGrain(int gx, int gz) {
        return ((long) gx << 32) | (gz & 0xFFFFFFFFL);
    }

    /** Whether a region file with mapped data exists (or was written) for this position. */
    public boolean isKnownRegion(int rx, int rz) {
        return knownRegions.contains(new RegionPos(rx, rz));
    }

    /** Set of packed level-3 tile grid cells that contain at least one mapped region. */
    public Set<Long> knownRegionsByGrain() {
        return knownGrains;
    }

    /**
     * Regions whose pixel data changed since the previous call. Call from the
     * render thread; writers keep adding during the swap.
     */
    public Set<RegionPos> drainUpdatedRegions() {
        if (updatedRegions.isEmpty()) return Set.of();
        Set<RegionPos> drained = new java.util.HashSet<>();
        for (RegionPos rp : updatedRegions) {
            if (updatedRegions.remove(rp)) drained.add(rp);
        }
        return drained;
    }

    public boolean isCave() { return cave; }

    public Path dimRoot() { return dimRoot; }

    public MapRegionData getIfCached(RegionPos pos) {
        MapRegionData d = cache.get(pos);
        if (d != null) lastAccess.put(pos, System.nanoTime());
        return d;
    }

    public MapRegionData getOrLoad(RegionPos pos) {
        MapRegionData cached = cache.get(pos);
        if (cached != null) { lastAccess.put(pos, System.nanoTime()); return cached; }
        MapRegionData loaded = loadFromDisk(pos);
        if (loaded != null) { cache.put(pos, loaded); lastAccess.put(pos, System.nanoTime()); evictIfNeeded(); return loaded; }
        MapRegionData fresh = new MapRegionData(pos);
        cache.put(pos, fresh);
        lastAccess.put(pos, System.nanoTime());
        evictIfNeeded();
        return fresh;
    }

    private void evictIfNeeded() {
        if (cache.size() <= MAX_CACHED_REGIONS) return;
        RegionPos oldest = null;
        long oldestT = Long.MAX_VALUE;
        for (Map.Entry<RegionPos, Long> e : lastAccess.entrySet()) {
            MapRegionData d = cache.get(e.getKey());
            if (d != null && d.isDirty()) continue;
            if (e.getValue() < oldestT) { oldestT = e.getValue(); oldest = e.getKey(); }
        }
        if (oldest != null) {
            cache.remove(oldest);
            lastAccess.remove(oldest);
        }
    }

    public void putChunkTile(int chunkX, int chunkZ, int[] tile16) {
        RegionPos rp = RegionPos.fromChunk(chunkX, chunkZ);
        MapRegionData region = getOrLoad(rp);
        region.putChunkTile(chunkX, chunkZ, tile16);
        addKnownRegion(rp);
        updatedRegions.add(rp);
    }

    private volatile long lastFlushMs = 0;
    private static final long FLUSH_THROTTLE_MS = 12000;

    public void flushDirty() {
        long now = System.currentTimeMillis();
        if (now - lastFlushMs < FLUSH_THROTTLE_MS) return;
        flushDirtyForced();
    }

    public void flushDirtyForced() {
        for (Map.Entry<RegionPos, MapRegionData> e : cache.entrySet()) {
            if (e.getValue().isDirty()) {
                saveToDisk(e.getValue());
                e.getValue().clearSaveFlag();
            }
        }
        lastFlushMs = System.currentTimeMillis();
    }

    public void flushRegion(RegionPos pos) {
        MapRegionData d = cache.get(pos);
        if (d != null && d.isDirty()) { saveToDisk(d); d.clearSaveFlag(); }
    }

    private Path binPath(RegionPos pos) { return dimRoot.resolve("r." + pos.rx() + "." + pos.rz() + ".bin"); }
    private Path pngPath(RegionPos pos) { return dimRoot.resolve(pos.fileName()); }

    private MapRegionData loadFromDisk(RegionPos pos) {
        Path bin = binPath(pos);
        if (Files.exists(bin)) {
            try { return loadBinary(bin, pos); } catch (IOException e) {
                AuraMapBridge.LOGGER.warn("failed to load bin {}", bin, e);
            }
        }
        Path png = pngPath(pos);
        if (Files.exists(png)) {
            try { return loadPng(png, pos); } catch (IOException e) {
                AuraMapBridge.LOGGER.warn("failed to load png {}", png, e);
            }
        }
        return null;
    }

    private MapRegionData loadBinary(Path file, RegionPos pos) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC) throw new IOException("bad magic " + Integer.toHexString(magic));
            byte ver = in.readByte();
            if (ver != VERSION) throw new IOException("bad version " + ver);
            int size = in.readInt();
            if (size != REGION_SIZE) throw new IOException("bad size " + size);
            int[] pixels = new int[PIXEL_COUNT];

            byte[] bytes = new byte[PIXEL_COUNT * 4];
            in.readFully(bytes);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            for (int i = 0; i < PIXEL_COUNT; i++) pixels[i] = bb.getInt();
            return new MapRegionData(pos, pixels);
        }
    }

    private MapRegionData loadPng(Path file, RegionPos pos) throws IOException {
        BufferedImage img = ImageIO.read(file.toFile());
        if (img == null) throw new IOException("ImageIO null");
        if (img.getWidth() != REGION_SIZE || img.getHeight() != REGION_SIZE) {
            AuraMapBridge.LOGGER.warn("unexpected png size {}x{} {}", img.getWidth(), img.getHeight(), file);
            throw new IOException("bad png size");
        }
        int[] pixels = new int[PIXEL_COUNT];
        img.getRGB(0, 0, REGION_SIZE, REGION_SIZE, pixels, 0, REGION_SIZE);

        MapRegionData data = new MapRegionData(pos, pixels);
        data.markDirty();
        return data;
    }

    private static final ThreadLocal<ByteBuffer> SAVE_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(PIXEL_COUNT * 4));

    private void saveToDisk(MapRegionData data) {
        Path file = binPath(data.pos);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(MAGIC);
                out.writeByte(VERSION);
                out.writeInt(REGION_SIZE);
                int[] pixels = data.pixels();
                ByteBuffer bb = SAVE_BUFFER.get();
                bb.clear();
                for (int p : pixels) bb.putInt(p);
                out.write(bb.array(), 0, bb.position());
                out.flush();
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Path legacyPng = pngPath(data.pos);
            if (Files.exists(legacyPng)) {
                try { Files.deleteIfExists(legacyPng); } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            AuraMapBridge.LOGGER.warn("failed to save bin {}", file, e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|') sb.append('_');
            else sb.append(c);
        }
        String out = sb.toString();
        if (out.length() > 64) out = out.substring(0, 64);
        if (out.isEmpty()) out = "default";
        return out;
    }
}
