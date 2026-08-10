package com.zephyr.client.configplusgui.config;

import com.google.gson.JsonObject;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;

/**
 * Implements the "Stealth Mode" panic feature. When activated it snapshots every module's
 * state (via {@link ConfigManager#writeModuleStates}) and disables all modules so nothing
 * remains visually or behaviorally active; when deactivated it restores the snapshot with
 * {@link ConfigManager#applyModuleStates}. The active flag is persisted through
 * {@link GlobalConfig}, but the snapshot itself lives only in memory - a session that ends
 * in Stealth Mode starts fresh next launch.
 */
public final class StealthManager {
    private static JsonObject snapshot;

    private StealthManager() {
    }

    /** Whether Stealth Mode is currently active (mirrors {@link GlobalConfig#stealthMode()}). */
    public static boolean isActive() {
        return GlobalConfig.stealthMode();
    }

    /**
     * Activates or deactivates Stealth Mode, snapshotting/restoring all modules as needed.
     *
     * @param active true to enter stealth (disable everything), false to exit (restore)
     */
    public static void setActive(boolean active) {
        if (active) {
            enter();
        } else {
            exit();
        }
    }

    /** Snapshots all module state and disables every module. Re-entering replaces the snapshot. */
    private static void enter() {
        if (GlobalConfig.stealthMode()) {
            snapshot = null;
            return;
        }

        GlobalConfig.stealthMode.set(true);
        snapshot = ConfigManager.writeModuleStates(ModuleManager.getModules());
        for (Module module : ModuleManager.getModules()) {
            module.setEnabled(false);
        }
        GlobalConfig.save();
    }

    /** Restores the snapshot taken on entry, discarding it afterwards. */
    private static void exit() {
        if (!GlobalConfig.stealthMode()) return;

        GlobalConfig.stealthMode.set(false);
        if (snapshot != null) {
            ConfigManager.applyModuleStates(snapshot, ModuleManager.getModules(), true);
            snapshot = null;
        }
        GlobalConfig.save();
    }
}
