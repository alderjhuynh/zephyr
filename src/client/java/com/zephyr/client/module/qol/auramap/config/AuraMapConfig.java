package com.zephyr.client.module.qol.auramap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AuraMapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "auramap.json";

    public boolean caveModeAllowed = true;
    public int caveModeDepth = 30;
    public boolean lighting = true;
    public boolean terrainShading = true;
    public boolean terrainDepth = true;
    public boolean biomeBlending = true;
    public boolean showCoordinates = true;
    public boolean showBiome = true;
    @Deprecated public boolean showZoomButtons = false;
    public double waypointsScale = 1.0;
    public boolean waypointsEnabled = true;
    public boolean waypointLabels = true;
    public boolean waypointDeathpoints = true;
    public int waypointMaxDistance = 0;
    public boolean waypointWorldLabels = true;
    public int waypointOpacity = 90;
    public double waypointIconScale = 1.0;
    public double waypointNameScale = 1.0;
    public double waypointDistanceScale = 1.0;
    public int waypointMinWorldDistance = 0;
    public boolean waypointShortDistances = false;
    public int waypointKmThreshold = 1000;
    public int waypointDistancePrecision = 0;
    public double waypointLookAngleDeg = 6.0;
    public double waypointLookAngleVerticalDeg = 8.0;
    public boolean waypointKeepNames = false;
    public float caveToggleSeconds = 1.0f;
    public int mapWritingDistance = -1;
    public boolean openingAnimation = false;
    public boolean displayZoom = true;
    public boolean minimapRadar = true;
    public boolean minimapEnabled = true;
    public int minimapSize = 128;
    public double minimapZoom = 1.0;

    private transient Path file;

    public static AuraMapConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        AuraMapConfig cfg = new AuraMapConfig();
        cfg.file = path;
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                AuraMapConfig loaded = GSON.fromJson(json, AuraMapConfig.class);
                if (loaded != null) {
                    loaded.file = path;

                    loaded.caveModeDepth = clamp(loaded.caveModeDepth, 1, 64);
                    loaded.caveToggleSeconds = clamp(loaded.caveToggleSeconds, 0f, 10f);
                    loaded.waypointsScale = clamp(loaded.waypointsScale, 0.5, 5.0);
                    loaded.waypointMaxDistance = clamp(loaded.waypointMaxDistance, 0, 100000);
                    loaded.minimapSize = clamp(loaded.minimapSize, 64, 256);
                    loaded.minimapZoom = clamp(loaded.minimapZoom, 0.25, 8.0);
                    loaded.waypointOpacity = clamp(loaded.waypointOpacity, 10, 100);
                    loaded.waypointIconScale = clamp(loaded.waypointIconScale, 0.5, 4.0);
                    loaded.waypointNameScale = clamp(loaded.waypointNameScale, 0.5, 4.0);
                    loaded.waypointDistanceScale = clamp(loaded.waypointDistanceScale, 0.5, 4.0);
                    loaded.waypointMinWorldDistance = clamp(loaded.waypointMinWorldDistance, 0, 100000);
                    loaded.waypointKmThreshold = clamp(loaded.waypointKmThreshold, -1, 100000);
                    loaded.waypointDistancePrecision = clamp(loaded.waypointDistancePrecision, 0, 3);
                    loaded.waypointLookAngleDeg = clamp(loaded.waypointLookAngleDeg, 1.0, 45.0);
                    loaded.waypointLookAngleVerticalDeg = clamp(loaded.waypointLookAngleVerticalDeg, 1.0, 45.0);
                    return loaded;
                }
            } catch (IOException e) {
                AuraMapBridge.LOGGER.warn("[auramap] failed to read config, using defaults", e);
            }
        }
        cfg.save();
        return cfg;
    }

    public void save() {
        if (file == null) file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            AuraMapBridge.LOGGER.warn("[auramap] failed to write config", e);
        }
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
