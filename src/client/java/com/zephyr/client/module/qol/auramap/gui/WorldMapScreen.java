package com.zephyr.client.module.qol.auramap.gui;

import com.zephyr.client.module.qol.auramap.config.AuraMapConfig;
import com.zephyr.client.module.qol.auramap.render.MapTextureCache;
import com.zephyr.client.module.qol.auramap.storage.RegionFileStorage;
import com.zephyr.client.module.qol.auramap.storage.RegionPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

public class WorldMapScreen extends Screen {
    private static final Component TITLE = Component.literal("AuraMap");

    private final RegionFileStorage storage;
    private final RegionFileStorage caveStorage;
    private final MapTextureCache cache;
    private final MapTextureCache caveCache;
    private final AuraMapConfig config;

    private double centerX;
    private double centerZ;
    private double zoom = 1.0;

    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 8.0;

    private double dragStartX, dragStartZ;
    private double dragMouseX, dragMouseY;
    private boolean dragging;
    private double animProgress = 1.0;

    public WorldMapScreen(RegionFileStorage storage, RegionFileStorage caveStorage,
            AuraMapConfig config, double initialX, double initialZ) {
        super(TITLE);
        this.storage = storage;
        this.caveStorage = caveStorage;
        this.config = config;
        this.cache = new MapTextureCache(storage);
        this.caveCache = caveStorage != null ? new MapTextureCache(caveStorage, "map_cave") : null;
        this.centerX = initialX;
        this.centerZ = initialZ;
    }

    private MapTextureCache activeCache() {
        if (caveCache != null) {
            try {
                if (com.zephyr.client.module.qol.auramap.AuraMapController.CAVE.isCave()) return caveCache;
            } catch (Throwable ignored) {}
        }
        return cache;
    }

    @Override
    protected void init() {
        animProgress = config.openingAnimation ? 0.0 : 1.0;
    }

    @Override
    public void tick() {
        if (animProgress < 1.0) animProgress = Math.min(1.0, animProgress + 0.12);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {

        g.fill(0, 0, width, height, 0xDD0A0A0A);
        float scale = (float) easeOutCubic(animProgress);
        Matrix3x2fStack pose = g.pose();
        boolean scaled = scale != 1f;
        if (scaled) {
            pose.pushMatrix();
            pose.translate(width / 2.0f, height / 2.0f);
            pose.scale(scale, scale);
            pose.translate(-width / 2.0f, -height / 2.0f);
        }
        try {
            renderMap(g);
            renderWaypoints(g, delta);
        } finally {
            if (scaled) pose.popMatrix();
        }
        renderHud(g);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {

    }

    private void renderMap(GuiGraphicsExtractor g) {
        int screenCx = width / 2;
        int screenCz = height / 2;

        double halfWBlocks = (width / zoom) * 0.5;
        double halfHBlocks = (height / zoom) * 0.5;
        int minBlockX = (int) Math.floor(centerX - halfWBlocks);
        int maxBlockX = (int) Math.ceil(centerX + halfWBlocks);
        int minBlockZ = (int) Math.floor(centerZ - halfHBlocks);
        int maxBlockZ = (int) Math.ceil(centerZ + halfHBlocks);

        RegionPos minR = RegionPos.fromBlock(minBlockX, minBlockZ);
        RegionPos maxR = RegionPos.fromBlock(maxBlockX, maxBlockZ);

        double baseX = screenCx - centerX * zoom;
        double baseZ = screenCz - centerZ * zoom;
        MapTextureCache active = activeCache();
        active.beginFrame();
            for (int rz = minR.rz(); rz <= maxR.rz(); rz++) {
            for (int rx = minR.rx(); rx <= maxR.rx(); rx++) {
                RegionPos rp = new RegionPos(rx, rz);
                var texId = active.getOrUpload(rp);
                int regionOriginX = rx * RegionPos.REGION_BLOCK_SIZE;
                int regionOriginZ = rz * RegionPos.REGION_BLOCK_SIZE;
                int ix0 = (int) Math.floor(baseX + regionOriginX * zoom);
                int iz0 = (int) Math.floor(baseZ + regionOriginZ * zoom);
                int ix1 = (int) Math.floor(baseX + (regionOriginX + RegionPos.REGION_BLOCK_SIZE) * zoom);
                int iz1 = (int) Math.floor(baseZ + (regionOriginZ + RegionPos.REGION_BLOCK_SIZE) * zoom);
                int iw = ix1 - ix0;
                int ih = iz1 - iz0;
                if (iw <= 0 || ih <= 0) continue;
                if (ix0 + iw < 0 || iz0 + ih < 0 || ix0 > width || iz0 > height) continue;
                if (texId == null) {
                    g.fill(ix0, iz0, ix0 + iw, iz0 + ih, 0xFF14181D);
                    continue;
                }
                try {
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texId,
                            ix0, iz0, 0f, 0f, iw, ih, 512, 512, 512, 512);
                } catch (Throwable t) {
                    g.fill(ix0, iz0, ix0 + iw, iz0 + ih, 0xFF334455);
                }
            }
        }
    }

    private void renderWaypoints(GuiGraphicsExtractor g, float delta) {
        int screenCx = width / 2;
        int screenCz = height / 2;
        double baseX = screenCx - centerX * zoom;
        double baseZ = screenCz - centerZ * zoom;
        try {
            var mc = Minecraft.getInstance();
            double ppx = baseX, ppy = 0, ppz = baseZ;
            boolean hasPlayer = false;
            if (mc.player != null) {
                double t = Math.max(0.0, Math.min(1.0, delta));
                ppx = Mth.lerp(t, mc.player.xo, mc.player.getX());
                ppy = Mth.lerp(t, mc.player.yo, mc.player.getY());
                ppz = Mth.lerp(t, mc.player.zo, mc.player.getZ());
                hasPlayer = true;
            }
            if (hasPlayer) {
                com.zephyr.client.module.qol.auramap.waypoint.render.WaypointMinimapOverlay.renderWorldMap(
                        g, baseX, baseZ, zoom, width, height, ppx, ppy, ppz);
            } else {
                com.zephyr.client.module.qol.auramap.waypoint.render.WaypointMinimapOverlay.renderWorldMap(g, baseX, baseZ, zoom, width, height);
            }
        } catch (Throwable ignored) {}
    }

    private void renderHud(GuiGraphicsExtractor g) {
        var mc = Minecraft.getInstance();
        var font = mc.font;
        var p = mc.player;
        int y = 6;
        if (config.showCoordinates && p != null) {
            String coords = String.format("x: %d  z: %d  y: %d", (int) p.getX(), (int) p.getY(), (int) p.getZ());
            g.text(font, coords, width / 2 - font.width(coords) / 2, y, 0xFFFFFF, true);
            y += 10;
        }
        if (config.showBiome && mc.level != null && p != null) {
            var biome = mc.level.getBiome(p.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
            String bText = "biome: " + biome;
            g.text(font, bText, 6, height - 12, 0xAAAAAA, true);
        }
        if (config.displayZoom) {
            String zoomText = String.format("zoom %.2fx", zoom);
            try {
                if (com.zephyr.client.module.qol.auramap.AuraMapController.CAVE.isCave()) zoomText = "cave  " + zoomText;
            } catch (Throwable ignored) {}
            g.text(font, zoomText, width - font.width(zoomText) - 6, height - 12, 0xFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == 1) {
            double blockX = centerX + (event.x() - width / 2.0) / zoom;
            double blockZ = centerZ + (event.y() - height / 2.0) / zoom;
            var mc = Minecraft.getInstance();
            int y = mc.player != null ? (int) mc.player.getY() : 64;
            mc.setScreenAndShow(new com.zephyr.client.module.qol.auramap.waypoint.gui.WaypointEditScreen(
                    this, null, (int) Math.floor(blockX), y, (int) Math.floor(blockZ)));
            return true;
        }
        if (event.button() == 2) {
            double blockX = centerX + (event.x() - width / 2.0) / zoom;
            double blockZ = centerZ + (event.y() - height / 2.0) / zoom;
            var mc = Minecraft.getInstance();
            int y = mc.player != null ? (int) mc.player.getY() : 64;
            com.zephyr.client.module.qol.auramap.waypoint.WaypointManager.get()
                    .setTemporary((int) Math.floor(blockX), y, (int) Math.floor(blockZ), false);
            return true;
        }
        if (event.button() == 0) {
            dragging = true;
            dragStartX = centerX;
            dragStartZ = centerZ;
            dragMouseX = event.x();
            dragMouseY = event.y();
            return true;
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging) {
            centerX = dragStartX - (event.x() - dragMouseX) / zoom;
            centerZ = dragStartZ - (event.y() - dragMouseY) / zoom;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double oldZoom = zoom;
        double factor = Math.pow(1.15, scrollY);
        zoom = Mth.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        if (zoom != oldZoom) {
            double mouseBlockX = centerX + (mouseX - width / 2.0) / oldZoom;
            double mouseBlockZ = centerZ + (mouseY - height / 2.0) / oldZoom;
            centerX = mouseBlockX - (mouseX - width / 2.0) / zoom;
            centerZ = mouseBlockZ - (mouseY - height / 2.0) / zoom;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int code = event.key();
        if (code == 256 || code == 259) {
            onClose();
            return true;
        }
        if (code == 93 || code == 61 || code == 334) { zoomIn(); return true; }
        if (code == 47 || code == 45 || code == 333) { zoomOut(); return true; }
        if (code == 66) {
            Minecraft.getInstance().setScreenAndShow(
                    new com.zephyr.client.module.qol.auramap.waypoint.gui.WaypointsScreen(this));
            return true;
        }
        if (code == 78) {
            var mc = Minecraft.getInstance();
            int y = mc.player != null ? (int) mc.player.getY() : 64;
            mc.setScreenAndShow(new com.zephyr.client.module.qol.auramap.waypoint.gui.WaypointEditScreen(
                    this, null, (int) Math.floor(centerX), y, (int) Math.floor(centerZ)));
            return true;
        }
        if (code == 84 || code == 88) {
            com.zephyr.client.module.qol.auramap.waypoint.WaypointManager.get().clearTemporary();
            return true;
        }
        return super.keyPressed(event);
    }

    private void zoomIn() { zoom = Mth.clamp(zoom * 1.25, MIN_ZOOM, MAX_ZOOM); }
    private void zoomOut() { zoom = Mth.clamp(zoom / 1.25, MIN_ZOOM, MAX_ZOOM); }

    @Override
    public void onClose() {
        cache.close();
        if (caveCache != null) {
            try { caveCache.close(); } catch (Exception ignored) {}
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static double easeOutCubic(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        double u = 1 - t;
        return 1 - u * u * u;
    }
}
