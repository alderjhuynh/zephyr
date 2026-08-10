package com.zephyr.client.module.qol.seedcracker.config;

import com.seedfinding.mccore.version.MCVersion;
import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.seedcracker.util.FeatureToggle;

/**
 * Central configuration/settings holder for the seedcracker module.
 *
 * <p>Exposes the enabled/disabled state of every findable feature, the rendering mode, debug and
 * anti-x-ray options, and the {@link MCVersion} the cracker is currently operating against.
 * Accessed via the {@link #get()} singleton.
 */
public class Config {
    private static final Config INSTANCE = new Config();

    public final FeatureToggle buriedTreasure = new FeatureToggle(true);
    public final FeatureToggle desertTemple = new FeatureToggle(true);
    public final FeatureToggle endCity = new FeatureToggle(true);
    public final FeatureToggle jungleTemple = new FeatureToggle(true);
    public final FeatureToggle monument = new FeatureToggle(true);
    public final FeatureToggle swampHut = new FeatureToggle(true);
    public final FeatureToggle shipwreck = new FeatureToggle(true);
    public final FeatureToggle outpost = new FeatureToggle(true);
    public final FeatureToggle igloo = new FeatureToggle(true);
    public final FeatureToggle trialChambers = new FeatureToggle(true);
    public final FeatureToggle endPillars = new FeatureToggle(true);
    public final FeatureToggle endGateway = new FeatureToggle(false);
    public final FeatureToggle dungeon = new FeatureToggle(true);
    public final FeatureToggle emeraldOre = new FeatureToggle(false);
    public final FeatureToggle desertWell = new FeatureToggle(false);
    public final FeatureToggle warpedFungus = new FeatureToggle(false);
    public final FeatureToggle biome = new FeatureToggle(false);

    public RenderType render = RenderType.XRAY;
    public boolean active = true;
    public boolean debug = false;
    public boolean antiXrayBypass = true;

    private MCVersion version = MCVersion.latest();

    /**
     * Returns the shared seedcracker configuration singleton.
     *
     * @return the global {@link Config} instance
     */
    public static Config get() {
        return INSTANCE;
    }

    /**
     * @return the Minecraft version features are currently built for
     */
    public MCVersion getVersion() {
        return version;
    }

    /**
     * Updates the active Minecraft version, reinitialising all {@link Features} when it changes.
     *
     * @param version the new Minecraft version
     */
    public void setVersion(MCVersion version) {
        if (this.version == version) return;
        this.version = version;
        Features.init(version);
    }

    /** Rendering mode for found structure/decorator outlines. */
    public enum RenderType {
        OFF, ON, XRAY
    }
}
