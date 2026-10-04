package com.zephyr.client.module.qol.auramap;

import com.zephyr.client.module.qol.auramap.config.AuraMapConfig;
import com.zephyr.client.module.qol.auramap.gui.WorldMapScreen;
import com.zephyr.client.module.qol.auramap.minimap.MinimapDataStore;
import com.zephyr.client.module.qol.auramap.render.MinimapRenderer;
import com.zephyr.client.module.qol.auramap.storage.RegionFileStorage;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointManager;
import com.zephyr.client.module.qol.auramap.waypoint.gui.WaypointEditScreen;
import com.zephyr.client.module.qol.auramap.waypoint.gui.WaypointsScreen;
import com.zephyr.client.module.qol.auramap.waypoint.render.WaypointWorldRenderer;
import com.zephyr.client.module.qol.auramap.world.CaveModeTracker;
import com.zephyr.client.module.qol.auramap.world.DimensionContext;
import com.zephyr.client.module.qol.auramap.world.MapUpdateQueue;
import net.minecraft.client.Minecraft;

/**
 * Owns the ported AuraMap runtime: background chunk capture, region storage,
 * minimap data, cave detection and waypoints. This is a direct adaptation of
 * the standalone mod's client entrypoint, reworked for Zephyr's module system:
 *
 * <ul>
 *   <li>Initialized once from {@code ZephyrClient} via {@link #init()}.</li>
 *   <li>Ticked only while the {@code AuraMap} QoL module is enabled, via
 *   {@link #tick(Minecraft)}; that module polls its own {@code KeybindSetting}s
 *   and calls {@link #openMap}/{@link #openWaypoints}/{@link #addWaypointAtPlayer}.</li>
 *   <li>Renderers gate on {@link #isActive()} so disabling the module hides the
 *   minimap and in-world waypoint labels immediately.</li>
 * </ul>
 */
public final class AuraMapController {
    public static AuraMapConfig CONFIG;
    public static final MapUpdateQueue UPDATE_QUEUE = new MapUpdateQueue();
    public static final CaveModeTracker CAVE = new CaveModeTracker();
    private static RegionFileStorage currentStorage;
    private static RegionFileStorage currentCaveStorage;
    private static final MinimapDataStore MINIMAP_STORE = new MinimapDataStore();
    private static final MinimapRenderer MINIMAP_RENDERER = new MinimapRenderer();
    private static int sampleCursor = 0;
    private static volatile boolean active;
    private static boolean initialized;

    private AuraMapController() {
    }

    /** One-time setup: config and the waypoint world renderer. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        CONFIG = AuraMapConfig.load();
        WaypointWorldRenderer.register();
        UPDATE_QUEUE.setMinimapStore(MINIMAP_STORE);
        AuraMapBridge.LOGGER.info("[auramap] ported controller init (zephyr qol module)");
    }

    /** Called by the AuraMap module's onEnable/onDisable to gate renderers and input. */
    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            deactivate();
        }
    }

    /** Whether the module is currently driving map capture and rendering. */
    public static boolean isActive() {
        return active;
    }

    /** Flushes pending writes and drops per-world state (called on disable / disconnect). */
    public static void deactivate() {
        if (currentStorage != null) {
            try {
                UPDATE_QUEUE.flushAsyncForced();
            } catch (Throwable ignored) {
            }
            WaypointManager.get().saveNow();
            WaypointManager.get().clearContext();
            currentStorage = null;
            currentCaveStorage = null;
            CAVE.reset();
            MINIMAP_STORE.clear();
            MINIMAP_RENDERER.close();
        }
    }

    /** Per-tick driver; no-ops unless the module is active. */
    public static void tick(Minecraft mc) {
        if (!active || CONFIG == null) return;
        if (mc.player == null || mc.level == null) {
            if (currentStorage != null) {
                UPDATE_QUEUE.flushAsyncForced();
                WaypointManager.get().saveNow();
                WaypointManager.get().clearContext();
                currentStorage = null;
                currentCaveStorage = null;
                CAVE.reset();
                MINIMAP_STORE.clear();
                MINIMAP_RENDERER.close();
            }
            return;
        }
        if (currentStorage == null || !isStorageCurrent(mc)) {
            currentStorage = DimensionContext.storageFor(mc.level);
            currentCaveStorage = DimensionContext.caveStorageFor(mc.level);
            UPDATE_QUEUE.setStorage(currentStorage);
            UPDATE_QUEUE.setCaveStorage(currentCaveStorage);
            UPDATE_QUEUE.setMinimapStore(MINIMAP_STORE);
            MINIMAP_STORE.clear();
            MINIMAP_RENDERER.close();
            CAVE.reset();
            WaypointManager.get().setContext(currentStorage.dimRoot(), DimensionContext.worldId());
        }
        updateCaveMode(mc);
        if (CONFIG.waypointDeathpoints) {
            WaypointManager.get().tickDeathTracking(mc);
        }
        if (mc.level != null && mc.level.getGameTime() % 4 == 0) {
            sampleNearbyChunksThrottled(mc, 8);
        }
        if (mc.level != null && mc.player != null && currentStorage != null) {
            int pcx = mc.player.chunkPosition().x();
            int pcz = mc.player.chunkPosition().z();
            int radius = mc.options.getEffectiveRenderDistance();
            radius = Math.max(4, Math.min(radius, 16));
            if (CONFIG.mapWritingDistance >= 0) radius = Math.min(radius, CONFIG.mapWritingDistance);
            UPDATE_QUEUE.drainWithBudget(mc.level, pcx, pcz, radius);
        }
    }

    private static boolean isStorageCurrent(Minecraft mc) {
        if (mc.level == null || currentStorage == null) return false;
        var expected = DimensionContext.storageFor(mc.level);
        if (!expected.dimRoot().equals(currentStorage.dimRoot())) return false;
        if (currentCaveStorage == null) return false;
        var expectedCave = DimensionContext.caveStorageFor(mc.level);
        return expectedCave.dimRoot().equals(currentCaveStorage.dimRoot());
    }

    private static void updateCaveMode(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        try {
            CAVE.tick(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ(), CONFIG);
        } catch (Exception e) {
            AuraMapBridge.LOGGER.warn("[auramap] cave detection failed", e);
            CAVE.reset();
        }
        int depth = CONFIG != null ? Math.max(1, Math.min(64, CONFIG.caveModeDepth)) : 30;
        UPDATE_QUEUE.setCaveState(new MapUpdateQueue.CaveState(CAVE.isCave(), CAVE.current(), depth));
        MINIMAP_STORE.setCaveActive(CAVE.isCave());
    }

    private static void sampleNearbyChunksThrottled(Minecraft mc, int budget) {
        if (mc.level == null || mc.player == null) return;
        int pcx = mc.player.chunkPosition().x();
        int pcz = mc.player.chunkPosition().z();
        int radius = mc.options.getEffectiveRenderDistance();
        radius = Math.max(4, Math.min(radius, 16));
        if (CONFIG.mapWritingDistance >= 0) radius = Math.min(radius, CONFIG.mapWritingDistance);
        int diameter = radius * 2 + 1;
        int total = diameter * diameter;
        for (int i = 0; i < budget; i++) {
            int idx = (sampleCursor + i) % total;
            int dx = (idx % diameter) - radius;
            int dz = (idx / diameter) - radius;
            int cx = pcx + dx;
            int cz = pcz + dz;
            var chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
            if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk lc) {
                UPDATE_QUEUE.enqueue(lc);
            }
        }
        sampleCursor = (sampleCursor + budget) % total;
    }

    /** Opens the fullscreen map at the player's position, flushing pending writes first. */
    public static void openMap(Minecraft mc) {
        if (currentStorage == null && mc.level != null) {
            currentStorage = DimensionContext.storageFor(mc.level);
            UPDATE_QUEUE.setStorage(currentStorage);
        }
        if (currentCaveStorage == null && mc.level != null) {
            currentCaveStorage = DimensionContext.caveStorageFor(mc.level);
            UPDATE_QUEUE.setCaveStorage(currentCaveStorage);
        }
        if (currentStorage == null) {
            AuraMapBridge.LOGGER.warn("[auramap] no storage available to open map");
            return;
        }
        UPDATE_QUEUE.flushAsync();
        double x = mc.player != null ? mc.player.getX() : 0;
        double z = mc.player != null ? mc.player.getZ() : 0;
        mc.setScreenAndShow(new WorldMapScreen(currentStorage, currentCaveStorage, CONFIG, x, z));
    }

    /** Opens the waypoint list. Called from the AuraMap module's "Open Waypoints" keybind setting. */
    public static void openWaypoints(Minecraft mc) {
        mc.setScreenAndShow(new WaypointsScreen(mc.gui.screen()));
    }

    /** Opens the waypoint editor prefilled with the player's position. Called from the module's "Add Waypoint" keybind setting. */
    public static void addWaypointAtPlayer(Minecraft mc) {
        if (mc.player == null) return;
        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());
        mc.setScreenAndShow(new WaypointEditScreen(mc.gui.screen(), null, x, y, z));
    }

    public static RegionFileStorage currentStorage() {
        return currentStorage;
    }

    public static RegionFileStorage currentCaveStorage() {
        return currentCaveStorage;
    }

    public static MinimapDataStore minimapStore() {
        return MINIMAP_STORE;
    }

    public static MinimapRenderer minimapRenderer() {
        return MINIMAP_RENDERER;
    }
}
