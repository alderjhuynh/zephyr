package com.zephyr.client.module.qol.auramap.minimap;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class MinimapTextureCache {
    private static final int MAX_UPLOADS_PER_FRAME = 2;
    private static final int TEX_SIZE = MinimapChunkData.SIZE;

    private final MinimapDataStore store;

    private static final class Entry {
        final DynamicTexture texture;
        final Identifier id;
        Entry(DynamicTexture t, Identifier id) { this.texture = t; this.id = id; }
    }

    private final Map<MinimapChunkKey, Entry> entries = new HashMap<>();
    private int uploadsThisFrame;

    public MinimapTextureCache(MinimapDataStore store) {
        this.store = store;
    }

    public void beginFrame() {
        uploadsThisFrame = 0;
    }

    public Identifier getOrUpload(MinimapChunkKey key) {
        MinimapChunkData data = store.getIfPresent(key);
        Entry e = entries.get(key);
        if (data == null) return e != null ? e.id : null;
        if (!data.hasSomething()) return e != null ? e.id : null;

        if (e == null) {
            if (uploadsThisFrame >= MAX_UPLOADS_PER_FRAME) return null;
            uploadsThisFrame++;
            DynamicTexture tex = createTexture(data.pixels());
            Identifier id = AuraMapBridge.id("minimap/c_" + key.mx() + "_" + key.mz());
            Minecraft.getInstance().getTextureManager().register(id, tex);
            entries.put(key, new Entry(tex, id));
            data.clearDirty();
            return id;
        }
        if (data.isDirty()) {
            if (uploadsThisFrame >= MAX_UPLOADS_PER_FRAME) return e.id; // draw stale, upload next frame
            uploadsThisFrame++;
            updateTexture(e.texture, data.pixels());
            data.clearDirty();
        }
        return e.id;
    }

    public void prune(MinimapChunkKey center, int radiusChunks) {
        entries.entrySet().removeIf(en -> {
            MinimapChunkKey k = en.getKey();
            int dx = Math.abs(k.mx() - center.mx());
            int dz = Math.abs(k.mz() - center.mz());
            if (Math.max(dx, dz) > radiusChunks + 1) {
                try { Minecraft.getInstance().getTextureManager().release(en.getValue().id); } catch (Exception ignored) {}
                try { en.getValue().texture.close(); } catch (Exception ignored) {}
                return true;
            }
            return false;
        });
    }

    public void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        for (Entry e : entries.values()) {
            try { tm.release(e.id); } catch (Exception ignored) {}
            try { e.texture.close(); } catch (Exception ignored) {}
        }
        entries.clear();
    }

    private static DynamicTexture createTexture(int[] pixels) {
        NativeImage img = new NativeImage(TEX_SIZE, TEX_SIZE, false);
        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                img.setPixelABGR(x, y, abgrFromArgb(pixels[y * TEX_SIZE + x]));
            }
        }
        return new DynamicTexture(() -> "auramap minimap chunk", img);
    }

    private static void updateTexture(DynamicTexture tex, int[] pixels) {
        try {
            NativeImage img = tex.getPixels();
            if (img == null) return;
            for (int y = 0; y < TEX_SIZE; y++) {
                for (int x = 0; x < TEX_SIZE; x++) {
                    img.setPixelABGR(x, y, abgrFromArgb(pixels[y * TEX_SIZE + x]));
                }
            }
            tex.upload();
        } catch (Exception ex) {
            AuraMapBridge.LOGGER.warn("[auramap] minimap chunk upload failed", ex);
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
