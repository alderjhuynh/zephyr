package com.zephyr.client.module.qol;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.KeybindSetting;
import com.zephyr.client.configplusgui.setting.StringSetting;
import com.zephyr.client.module.qol.seedcracker.api.SeedCrackerAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.net.URI;
import java.util.Locale;

/**
 * One-click bridge to the Chunk Base seed map, opened in the system browser.
 *
 * <p>Minecraft's rendering surface cannot host web content (no web view is
 * available to mods), so instead of trying to embed
 * {@literal chunkbase.com/apps/seed-map} as an iframe this module opens it as a
 * normal browser tab, pre-filled with the world's seed via the URL fragment
 * ({@code https://www.chunkbase.com/apps/seed-map#<seed>}), which Chunk Base
 * reads on load. The seed is resolved in priority order:
 * <ol>
 *     <li>the manual seed override setting, when filled in (lets players view
 *     seeds they typed by hand, e.g. from a server admin or an older world);</li>
 *     <li>the seed recovered by the {@link Seedcracker} — this module registers
 *     itself as a {@link SeedCrackerAPI} entrypoint and keeps the last cracked
 *     seed for the current server session;</li>
 *     <li>the seed of the integrated server in singleplayer, served straight
 *     from the overworld's world-generation data.</li>
 * </ol>
 *
 * <p>The cracked seed is reported with a toast and cleared on disconnect (same
 * lifecycle hook the Seedcracker uses), so a seed from a previous server can
 * never leak into the next one.
 */
public final class SeedMap extends Module implements SeedCrackerAPI {
    public static final SeedMap INSTANCE = new SeedMap();

    private static final String SEED_MAP_URL = "https://www.chunkbase.com/apps/seed-map#";
    private static final int OK_COLOR = 0xFF55FF55;
    private static final int ERROR_COLOR = 0xFFFF5555;

    private final StringSetting seedOverride = new StringSetting("Seed Override", "");
    private final KeybindSetting openSeedMapKey = new KeybindSetting("Open Seed Map", InputConstants.KEY_J);

    /** Latest seed pushed by the Seedcracker; -1 means none for the current session. */
    private volatile long crackedSeed = -1L;

    private SeedMap() {
        super("Seed Map",
                "Opens the Chunk Base seed map in your browser, pre-filled with the world's seed",
                Category.QOL, true);
        addSetting(seedOverride);
        addSetting(openSeedMapKey);
        Seedcracker.entrypoints.add(this);
    }

    /** Returns the singleton instance of this module. */
    public static SeedMap get() {
        return INSTANCE;
    }

    /**
     * Clears the stored cracked seed. Called from the client-level disconnect
     * mixin so a seed cracked on one server is never reused on the next.
     */
    public void clearSeed() {
        this.crackedSeed = -1L;
    }

    /** Stores the seed pushed by the Seedcracker and reports it as a toast. */
    @Override
    public void pushWorldSeed(long seed) {
        this.crackedSeed = seed;
        NotificationManager.notifyText("Seed Map — seed saved (" + seed + ")", OK_COLOR);
    }

    /** Resolves the seed to show, falling back through the sources listed in the class doc. */
    private String resolveSeed(Minecraft client) {
        String manual = seedOverride.get();
        if (manual != null && !manual.isBlank()) {
            try {
                long seed = Long.parseLong(manual.trim());
                if (seed != 0L) return Long.toString(seed);
            } catch (NumberFormatException ignored) {
            }
            NotificationManager.notifyText("Seed Map — invalid manual seed", ERROR_COLOR);
            return null;
        }
        long seed = crackedSeed;
        if (seed != -1L) return Long.toString(seed);
        // Client levels no longer expose the world seed (WorldGenLevel.getSeed is
        // server-side only), so in singleplayer read it from the integrated server.
        if (client != null && client.getSingleplayerServer() != null) {
            var overworld = client.getSingleplayerServer().getLevel(Level.OVERWORLD);
            if (overworld != null) return Long.toString(overworld.getSeed());
        }
        NotificationManager.notifyText("Seed Map — no seed available; crack the seed or enter it manually", ERROR_COLOR);
        return null;
    }

    /** Opens Chunk Base with the resolved seed, reporting failures as a toast. */
    private void openSeedMap(Minecraft client) {
        String seed = resolveSeed(client);
        if (seed == null) return;
        try {
            Blaze3D.openUri(URI.create(SEED_MAP_URL + seed));
        } catch (Exception e) {
            NotificationManager.notifyText("Seed Map — failed to open browser: "
                    + e.getClass().getSimpleName().toLowerCase(Locale.ROOT), ERROR_COLOR);
        }
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) return;
        if (openSeedMapKey.consumeClick()) {
            openSeedMap(client);
        }
    }
}
