package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.KeybindSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.qol.auramap.AuraMapController;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Ported AuraMap world map + minimap + waypoints, running as a QoL module.
 *
 * <p>While enabled the module captures chunks around the player into per-world
 * region files (shared with the standalone AuraMap layout), renders a live
 * rotating minimap in the top-left corner, draws waypoint markers in-world, and
 * exposes three rebindable per-action keys (fullscreen map, waypoint list,
 * add-waypoint) via {@link KeybindSetting}. Disabling hides all rendering
 * and stops background capture, flushing pending writes first.
 *
 * <p>Settings mirror the most useful {@code auramap.json} options and are synced
 * into it, so both the click-GUI and the raw config file stay valid.
 */
public final class AuraMap extends Module {
    public static final AuraMap INSTANCE = new AuraMap();

    private final BooleanSetting minimap = new BooleanSetting("Minimap", true);
    private final NumberSetting minimapSize = new NumberSetting("Minimap Size", 128.0, 64.0, 256.0, 4.0);
    private final NumberSetting minimapZoom = new NumberSetting("Minimap Zoom", 1.0, 0.25, 8.0, 0.25);
    private final BooleanSetting waypoints = new BooleanSetting("Waypoints", true);
    private final BooleanSetting waypointLabels = new BooleanSetting("Waypoint Labels", true);
    private final BooleanSetting worldLabels = new BooleanSetting("World Labels", true);
    private final BooleanSetting deathpoints = new BooleanSetting("Deathpoints", true);
    private final BooleanSetting caveMode = new BooleanSetting("Cave Mode", true);
    private final BooleanSetting mapLighting = new BooleanSetting("Map Lighting", true);
    private final BooleanSetting terrainShading = new BooleanSetting("Terrain Shading", true);
    private final NumberSetting mapWritingDistance =
            new NumberSetting("Map Writing Distance (-1 = unlimited)", -1.0, -1.0, 16.0, 1.0);
    private final KeybindSetting openMapKey = new KeybindSetting("Open Map", InputConstants.KEY_M);
    private final KeybindSetting waypointsKey = new KeybindSetting("Open Waypoints", InputConstants.KEY_B);
    private final KeybindSetting addWaypointKey = new KeybindSetting("Add Waypoint", InputConstants.KEY_N);

    private AuraMap() {
        super("AuraMap", "World map + minimap with waypoints and rebindable map keys", Category.QOL, true);
        addSetting(openMapKey);
        addSetting(waypointsKey);
        addSetting(addWaypointKey);
        addSetting(minimap);
        addSetting(minimapSize);
        addSetting(minimapZoom);
        addSetting(waypoints);
        addSetting(waypointLabels);
        addSetting(worldLabels);
        addSetting(deathpoints);
        addSetting(caveMode);
        addSetting(mapLighting);
        addSetting(terrainShading);
        addSetting(mapWritingDistance);
        // Seed GUI defaults from an existing auramap.json so a prior standalone
        // setup is respected; persisted modules.json still wins via ConfigManager.
        var cfg = AuraMapController.CONFIG;
        if (cfg != null) {
            pullFromConfig();
        }
    }

    /** Returns the singleton instance of this module. */
    public static AuraMap get() {
        return INSTANCE;
    }

    /** Copies the backing file config into the GUI settings. */
    private void pullFromConfig() {
        var cfg = AuraMapController.CONFIG;
        if (cfg == null) return;
        minimap.set(cfg.minimapEnabled);
        minimapSize.set(cfg.minimapSize);
        minimapZoom.set(cfg.minimapZoom);
        waypoints.set(cfg.waypointsEnabled);
        waypointLabels.set(cfg.waypointLabels);
        worldLabels.set(cfg.waypointWorldLabels);
        deathpoints.set(cfg.waypointDeathpoints);
        caveMode.set(cfg.caveModeAllowed);
        mapLighting.set(cfg.lighting);
        terrainShading.set(cfg.terrainShading);
        mapWritingDistance.set(cfg.mapWritingDistance);
    }

    /** Pushes the GUI settings into the backing file config (no disk write). */
    private void syncConfig() {
        var cfg = AuraMapController.CONFIG;
        if (cfg == null) return;
        cfg.minimapEnabled = minimap.get();
        cfg.minimapSize = (int) Math.round(minimapSize.get());
        cfg.minimapZoom = minimapZoom.get();
        cfg.waypointsEnabled = waypoints.get();
        cfg.waypointLabels = waypointLabels.get();
        cfg.waypointWorldLabels = worldLabels.get();
        cfg.waypointDeathpoints = deathpoints.get();
        cfg.caveModeAllowed = caveMode.get();
        cfg.lighting = mapLighting.get();
        cfg.terrainShading = terrainShading.get();
        cfg.mapWritingDistance = (int) Math.round(mapWritingDistance.get());
    }

    @Override
    protected void onEnable() {
        syncConfig();
        AuraMapController.setActive(true);
    }

    @Override
    protected void onDisable() {
        syncConfig();
        var cfg = AuraMapController.CONFIG;
        if (cfg != null) cfg.save();
        AuraMapController.setActive(false);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) return;
        // Self-heal the startup path: ConfigManager restores the enabled flag
        // silently, so onEnable may never have fired.
        AuraMapController.setActive(true);
        syncConfig();
        AuraMapController.tick(client);
        if (openMapKey.consumeClick()) {
            AuraMapController.openMap(client);
        }
        if (waypointsKey.consumeClick()) {
            AuraMapController.openWaypoints(client);
        }
        if (addWaypointKey.consumeClick()) {
            AuraMapController.addWaypointAtPlayer(client);
        }
    }
}
