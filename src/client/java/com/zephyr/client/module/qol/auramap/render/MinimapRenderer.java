package com.zephyr.client.module.qol.auramap.render;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.zephyr.client.module.qol.auramap.AuraMapController;
import com.zephyr.client.module.qol.auramap.minimap.MinimapChunkKey;
import com.zephyr.client.module.qol.auramap.minimap.MinimapProcessor;
import com.zephyr.client.module.qol.auramap.minimap.MinimapTextureCache;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

public final class MinimapRenderer implements HudElement {
    private static final int MARGIN = 8;
    private static final int UNEXPLORED = 0xFF14181D;

    private final MinimapProcessor processor = new MinimapProcessor();
    private MinimapTextureCache textures;
    private boolean firstFrame = true;
    private boolean lastCaveMode;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        try {
            if (!AuraMapController.isActive()) return;
            var mc = Minecraft.getInstance();
            var config = AuraMapController.CONFIG;
            if (config == null || !config.minimapEnabled) return;
            if (mc.player == null || mc.level == null) return;
            if (mc.gui.screen() != null) return;
            if (AuraMapController.currentStorage() == null) return;
            if (AuraMapController.minimapStore() == null) return;

            if (textures == null) {
                textures = new MinimapTextureCache(AuraMapController.minimapStore());
                lastCaveMode = AuraMapController.CAVE.isCave();
            }
            boolean caveMode = AuraMapController.CAVE.isCave();
            if (caveMode != lastCaveMode) {
                textures.clear();
                lastCaveMode = caveMode;
            }

            int size = Math.max(64, Math.min(256, config.minimapSize));
            float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            double px = Mth.lerp(partial, mc.player.xo, mc.player.getX());
            double pz = Mth.lerp(partial, mc.player.zo, mc.player.getZ());
            float yaw = Mth.rotLerp(partial, mc.player.yRotO, mc.player.getYRot());

            double zoom = processor.updateZoom(Mth.clamp((float) config.minimapZoom, 0.25f, 8.0f));
            int renderDist = Math.max(2, Math.min(32, mc.options.getEffectiveRenderDistance()));
            double blocksAcross = MinimapProcessor.blocksAcross(renderDist);
            boolean rotated = true;
            double radiusBlocks = MinimapProcessor.radiusBlocks(blocksAcross, zoom, rotated);
            double ppb = (size / blocksAcross) * zoom; // pixels per block
            ppb = Math.max(0.125, Math.min(16.0, ppb));

            int x0 = MARGIN;
            int y0 = MARGIN;

            g.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xCC000000);
            g.fill(x0, y0, x0 + size, y0 + size, UNEXPLORED);

            textures.beginFrame();

            int xFloored = Mth.floor(px);
            int zFloored = Mth.floor(pz);
            float fracX = (float) (px - xFloored);
            float fracZ = (float) (pz - zFloored);

            var range = MinimapProcessor.visibleRange(xFloored, zFloored, radiusBlocks);

            float cx = x0 + size / 2.0f;
            float cy = y0 + size / 2.0f;
            float rot = (float) Math.toRadians(180.0 - yaw);

            var pose = g.pose();
            g.enableScissor(x0, y0, x0 + size, y0 + size);
            pose.pushMatrix();
            try {
                pose.translate(cx, cy);
                pose.rotate(rot);
                pose.scale((float) ppb, (float) ppb);
                pose.translate(-fracX, -fracZ);

                for (int mx = range.minMx(); mx <= range.maxMx(); mx++) {
                    for (int mz = range.minMz(); mz <= range.maxMz(); mz++) {
                        MinimapChunkKey key = new MinimapChunkKey(mx, mz);
                        var texId = textures.getOrUpload(key);
                        int relX = key.minBlockX() - xFloored;
                        int relZ = key.minBlockZ() - zFloored;
                        if (texId == null) continue;
                        try {
                            g.blit(RenderPipelines.GUI_TEXTURED, texId,
                                    relX, relZ, 0.0f, 0.0f, 64, 64, 64, 64, 64, 64);
                        } catch (Throwable t) {
                            AuraMapBridge.LOGGER.warn("[auramap] minimap chunk blit failed", t);
                        }
                    }
                }
            } finally {
                pose.popMatrix();
                g.disableScissor();
            }
            try {
                com.zephyr.client.module.qol.auramap.waypoint.render.WaypointMinimapOverlay.renderMinimapLabels(
                        g, x0, y0, size, px, pz, yaw, zoom, radiusBlocks, ppb);
            } catch (Throwable ignored) {}

            try {
                MinimapChunkKey center = MinimapChunkKey.fromBlock(xFloored, zFloored);
                int radiusChunks = (int) Math.ceil(radiusBlocks / MinimapChunkKey.BLOCK_SIZE) + 1;
                textures.prune(center, radiusChunks);
                if (mc.level.getGameTime() % 100 == 0) {
                    AuraMapController.minimapStore().pruneFar(center.mx(), center.mz(), radiusChunks + 2);
                }
            } catch (Throwable ignored) {}

            g.outline(x0 - 2, y0 - 2, size + 4, size + 4, 0x88FFFFFF);

            if (firstFrame) {
                firstFrame = false;
                processor.snapZoom(zoom);
            }
        } catch (Throwable t) {
            AuraMapBridge.LOGGER.warn("[auramap] minimap render failed", t);
        }
    }

    public void close() {
        if (textures != null) {
            try { textures.clear(); } catch (Exception ignored) {}
            textures = null;
        }
        firstFrame = true;
    }
}
