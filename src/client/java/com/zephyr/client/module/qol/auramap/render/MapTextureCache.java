package com.zephyr.client.module.qol.auramap.render;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.zephyr.client.module.qol.auramap.storage.MapRegionData;
import com.zephyr.client.module.qol.auramap.storage.RegionFileStorage;
import com.zephyr.client.module.qol.auramap.storage.RegionPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MapTextureCache {
    private static final int MAX_FULL_UPLOADS_PER_FRAME = 2;
    private static final int MAX_TILE_UPLOADS_PER_FRAME = 8;

    private final RegionFileStorage storage;
    private final String namespace;
    private final Map<RegionPos, Entry> entries = new HashMap<>();
    private final Set<RegionPos> pendingLoad = new HashSet<>();
    private int fullUploadsThisFrame;
    private int tileUploadsThisFrame;
    private long frameId;

    private static class Entry {
        final DynamicTexture texture;
        final Identifier id;
        Entry(DynamicTexture t, Identifier id) { this.texture = t; this.id = id; }
    }

    public MapTextureCache(RegionFileStorage storage) {
        this(storage, "map");
    }

    public MapTextureCache(RegionFileStorage storage, String namespace) {
        this.storage = storage;
        this.namespace = namespace;
    }

    public Identifier getOrUpload(RegionPos pos) {
        Entry e = entries.get(pos);
        MapRegionData data = storage.getIfCached(pos);
        if (data == null) {
            requestLoadAsync(pos);
            if (e != null) return e.id;
            return null;
        }
        if (e == null) {
            if (fullUploadsThisFrame >= MAX_FULL_UPLOADS_PER_FRAME) {
                return null;
            }
            fullUploadsThisFrame++;
            DynamicTexture tex = createTexture(data.pixels());
            Identifier id = AuraMapBridge.id(namespace + "/r_" + pos.rx() + "_" + pos.rz());
            Minecraft.getInstance().getTextureManager().register(id, tex);
            e = new Entry(tex, id);
            entries.put(pos, e);
            data.clearTextureTiles();
            return e.id;
        }
        BitSet peek = data.peekDirtyTiles();
        boolean needsFull = data.isDirty() && peek == null;
        boolean needsTiles = peek != null && peek.cardinality() < 256;
        boolean needsFullTiles = peek != null && !needsTiles;
        if (needsTiles) {
            if (tileUploadsThisFrame < MAX_TILE_UPLOADS_PER_FRAME) {
                tileUploadsThisFrame++;
                BitSet tiles = data.consumeDirtyTiles();
                if (tiles != null) updateTiles(e.texture, data.pixels(), tiles);
            }
        } else if (needsFull || needsFullTiles) {
            if (fullUploadsThisFrame < MAX_FULL_UPLOADS_PER_FRAME) {
                fullUploadsThisFrame++;
                data.consumeDirtyTiles();
                updateTexture(e.texture, data.pixels());
            }
        }
        return e.id;
    }

    public void beginFrame() {
        fullUploadsThisFrame = 0;
        tileUploadsThisFrame = 0;
        frameId++;
    }

    private void requestLoadAsync(RegionPos pos) {
        synchronized (pendingLoad) {
            if (!pendingLoad.add(pos)) return;
        }
        Thread t = new Thread(() -> {
            try {
                storage.getOrLoad(pos);
            } finally {
                synchronized (pendingLoad) {
                    pendingLoad.remove(pos);
                }
            }
        }, "auramap-region-loader");
        t.setDaemon(true);
        t.start();
    }

    public void close() {
        var tm = Minecraft.getInstance().getTextureManager();
        for (Entry e : entries.values()) {
            try { tm.release(e.id); } catch (Exception ignored) {}
            try { e.texture.close(); } catch (Exception ignored) {}
        }
        entries.clear();
    }

    private static DynamicTexture createTexture(int[] pixels) {
        int size = RegionPos.REGION_PIXEL_SIZE;
        NativeImage img = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                img.setPixelABGR(x, y, abgrFromArgb(pixels[y * size + x]));
            }
        }
        return new DynamicTexture(() -> "auramap region", img);
    }

    private static void updateTiles(DynamicTexture tex, int[] pixels, BitSet tiles) {
        try {
            NativeImage img = tex.getPixels();
            if (img == null) return;
            int stride = RegionPos.REGION_PIXEL_SIZE;
            int tileSize = 16;
            int tilesPerRow = RegionPos.REGION_CHUNK_SIZE;
            for (int idx = tiles.nextSetBit(0); idx >= 0; idx = tiles.nextSetBit(idx + 1)) {
                int tx = idx % tilesPerRow;
                int tz = idx / tilesPerRow;
                int basePx = tx * tileSize;
                int basePz = tz * tileSize;
                for (int dy = 0; dy < tileSize; dy++) {
                    int py = basePz + dy;
                    int srcRow = py * stride + basePx;
                    for (int dx = 0; dx < tileSize; dx++) {
                        int px = basePx + dx;
                        int argb = pixels[srcRow + dx];
                        img.setPixelABGR(px, py, abgrFromArgb(argb));
                    }
                }
            }
            tex.upload();
        } catch (Exception ex) {
            AuraMapBridge.LOGGER.warn("[auramap] tile update failed", ex);
        }
    }

    private static void updateTexture(DynamicTexture tex, int[] pixels) {
        try {
            NativeImage img = tex.getPixels();
            if (img == null) return;
            int size = RegionPos.REGION_PIXEL_SIZE;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    img.setPixelABGR(x, y, abgrFromArgb(pixels[y * size + x]));
                }
            }
            tex.upload();
        } catch (Exception ex) {
            AuraMapBridge.LOGGER.warn("[auramap] texture update failed", ex);
        }
    }

    private static int abgrFromArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
