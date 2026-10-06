package com.zephyr.client.configplusgui.config;

import com.google.gson.JsonObject;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;

/**
 * Implements the "Hide Detectable Modules" panic-style toggle for Grim anticheat servers.
 * When activated it snapshots every Grim-detectable module's state (via
 * {@link ConfigManager#writeModuleStates}) and disables all of them so nothing
 * remains behaviorally active; when deactivated it restores the snapshot with
 * {@link ConfigManager#applyModuleStates}. The active flag is persisted through
 * {@link GlobalConfig}, but the snapshot itself lives only in memory - exactly like
 * {@link StealthManager} - so profiles on disk are never overwritten with the
 * forced-disabled state (see {@link ConfigManager#writeModuleStates} substitution).
 *
 * <p>How this differs from the permanent {@code hidden} module type (see
 * {@code HiddenModules}): hidden modules are always hidden unless a UUID/TestMode
 * unlock applies, never render in the main HUD, and live on the secret screen when
 * unlocked. Grim-hidden modules are normal modules that are only temporarily
 * filtered from UI lists while this toggle is on; when the toggle is off they
 * appear in the regular ClickGUI/keybinds/commands like any other module.
 */
public final class GrimHidingManager {
    private static JsonObject snapshot;

    private GrimHidingManager() {
    }

    /** Whether Hide Detectable Modules is currently active (mirrors {@link GlobalConfig#hideDetectableModules()}). */
    public static boolean isActive() {
        return GlobalConfig.hideDetectableModules();
    }

    /**
     * Whether the given module is currently suppressed by this filter: the global
     * toggle is on and the module is marked Grim-detectable.
     */
    public static boolean isHidden(Module module) {
        return isActive() && module.isGrimDetectable();
    }

    /**
     * Activates or deactivates hiding, snapshotting/restoring detectable modules as needed.
     *
     * @param active true to hide (disable detectables), false to unhide (restore)
     */
    public static void setActive(boolean active) {
        if (active) {
            enter();
        } else {
            exit();
        }
    }

    /** Snapshots detectable module state and disables every detectable module. Re-entering replaces the snapshot. */
    private static void enter() {
        if (GlobalConfig.hideDetectableModules()) {
            snapshot = null;
            return;
        }

        GlobalConfig.hideDetectableModules.set(true);
        snapshot = ConfigManager.writeModuleStates(detectableModules());
        for (Module module : detectableModules()) {
            module.setEnabled(false);
        }
        GlobalConfig.save();
    }

    /** Restores the snapshot taken on entry, discarding it afterwards. */
    private static void exit() {
        if (!GlobalConfig.hideDetectableModules()) return;

        GlobalConfig.hideDetectableModules.set(false);
        if (snapshot != null) {
            ConfigManager.applyModuleStates(snapshot, ModuleManager.getModules(), true);
            snapshot = null;
        }
        GlobalConfig.save();
    }

    /**
     * Re-applies hiding after config load when the flag persisted as ON across a
     * restart. There is no live snapshot to restore from after a shutdown, so the
     * freshly loaded module states become the new snapshot before disabling.
     * Called once from {@link ModuleManager#init()} after {@link ConfigManager#load}.
     */
    public static void reapplyAfterLoad() {
        if (!GlobalConfig.hideDetectableModules()) {
            return;
        }
        // Temporarily clear the flag so enter() takes the snapshot path.
        GlobalConfig.hideDetectableModules.set(false);
        enter();
    }

    /**
     * Merges a freshly applied profile's Grim-detectable entries into the active
     * hiding snapshot and re-force-disables live detectables. Called by
     * {@link ProfileManager#applyProfile} so switching profiles while hiding never
     * leaks detectable behavior, and unhiding later restores the new profile.
     */
    public static void refreshSnapshotFromProfile(JsonObject profileSnapshot) {
        if (!isActive() || snapshot == null || profileSnapshot == null) {
            // Still ensure live detectables stay off even without a snapshot.
            if (isActive()) {
                for (Module module : detectableModules()) {
                    module.setEnabled(false);
                }
            }
            return;
        }
        for (Module module : detectableModules()) {
            if (profileSnapshot.has(module.getName())) {
                snapshot.add(module.getName(),
                        profileSnapshot.getAsJsonObject(module.getName()).deepCopy());
            }
            module.setEnabled(false);
        }
    }

    /** Returns the live list of Grim-detectable modules. */
    private static java.util.List<Module> detectableModules() {
        java.util.List<Module> out = new java.util.ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isGrimDetectable()) {
                out.add(module);
            }
        }
        return out;
    }

    /**
     * Package-visible snapshot accessor so {@link ConfigManager#writeModuleStates}
     * can persist pre-hide states while hiding is active instead of the
     * forced-disabled live states. This is what keeps anarchy profiles intact
     * across autosaves, manual saves, and profile captures.
     */
    static JsonObject snapshotOrNull() {
        return snapshot;
    }
}
