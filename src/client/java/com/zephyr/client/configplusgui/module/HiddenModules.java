package com.zephyr.client.configplusgui.module;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for hidden modules feature. Hidden modules (where {@link Module#isHidden()} is true)
 * are only shown if either:
 * <ul>
 *   <li>the local player's UUID is {@code 1176ac92-5aec-4761-96ab-6ead488f8bb5}, or</li>
 *   <li>the boolean {@code TestMode} is true.</li>
 * </ul>
 * <p>Hidden modules never render in the main HUD (see {@code ModuleManager.getHudModules()});
 * when visible they are listed on the secret screen instead.
 */
public final class HiddenModules {
    // UUID allowed to see hidden modules
    private static final String ALLOWED_UUID = "1176ac92-5aec-4761-96ab-6ead488f8bb5";

    /**
     * Boolean toggle that forcibly shows hidden modules when true.
     * Mirrors {@link GlobalConfig#TestMode} for persistence; this field exists so the spec's
     * "boolean called TestMode" is directly visible on this class as well.
     */
    public static boolean TestMode = false;

    private HiddenModules() {
    }

    /** Whether hidden modules should currently be shown. */
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

    /** Whether the given module should be visible in UI lists. */
    public static boolean isVisible(Module module) {
        if (!module.isHidden()) return true;
        return shouldShowHidden();
    }

    /** Filters a module list to only those visible to the current player/mode. */
    public static List<Module> filterVisible(List<Module> modules) {
        if (shouldShowHidden()) return modules;
        return modules.stream().filter(m -> !m.isHidden()).collect(Collectors.toList());
    }
}
