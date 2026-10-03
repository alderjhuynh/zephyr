package com.zephyr.client.configplusgui.module;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.stream.Collectors;

public final class HiddenModules {
    // UUID allowed to see hidden modules
    private static final String ALLOWED_UUID = "1176ac92-5aec-4761-96ab-6ead488f8bb5";

    public static boolean TestMode = false;

    private HiddenModules() {
    }

    public static boolean shouldShowHidden() {
        // Check both this class's primitive toggle and the persisted GlobalConfig toggle
        if (TestMode) return true;
        if (GlobalConfig.TestMode.get()) return true;
        if (GlobalConfig.isTestMode()) return true;

        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                if (client.player != null && client.player.getUUID() != null) {
                    if (ALLOWED_UUID.equalsIgnoreCase(client.player.getUUID().toString())) {
                        return true;
                    }
                }
                // Also check user profile id when not in world (e.g. in menus)
                if (client.getUser() != null && client.getUser().getProfileId() != null) {
                    if (ALLOWED_UUID.equalsIgnoreCase(client.getUser().getProfileId().toString())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isVisible(Module module) {
        if (!module.isHidden()) return true;
        return shouldShowHidden();
    }

    public static List<Module> filterVisible(List<Module> modules) {
        if (shouldShowHidden()) return modules;
        return modules.stream().filter(m -> !m.isHidden()).collect(Collectors.toList());
    }
}
