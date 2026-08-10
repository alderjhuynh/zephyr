package com.zephyr.client.configplusgui.config;

import com.google.gson.JsonObject;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;

public final class StealthManager {
    private static JsonObject snapshot;

    private StealthManager() {
    }

    public static boolean isActive() {
        return GlobalConfig.stealthMode();
    }

    public static void setActive(boolean active) {
        if (active) {
            enter();
        } else {
            exit();
        }
    }

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
