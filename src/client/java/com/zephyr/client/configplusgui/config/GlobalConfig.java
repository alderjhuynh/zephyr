package com.zephyr.client.configplusgui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.hud.Corner;
import com.zephyr.client.configplusgui.hud.HudMode;
import com.zephyr.client.configplusgui.hud.PartyManager;
import com.zephyr.client.configplusgui.hud.ThemeColor;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.discord.DiscordPresenceManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central registry of client-wide preferences that are not owned by any single module:
 * theming (theme preset vs. custom HSV color), HUD overlay mode, toast placement and
 * lifetime, menu animation speed, autosave cadence, keybind conflict warnings, Discord
 * presence and Stealth Mode. Each value is exposed as a {@link Setting} so
 * {@link com.zephyr.client.configplusgui.screen.ConfigGuiScreen} can render generic rows,
 * while persistence (and any side effects like reconnecting Discord) is centralized in
 * the getter/setter pairs of this class. Data is stored in
 * {@code .minecraft/config/zephyr/client.json}.
 */
public final class GlobalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("client.json");

    private static ThemeColor themeColor = ThemeColor.LAVENDER;

    /**
     * Global settings rendered by {@link ConfigGuiScreen}'s generic setting rows.
     * Package-visible so the gui can dispatch on their concrete type; persistence is
     * owned by this class ({@link #save()}), which the gui calls after each change.
     */
    public static final BooleanSetting hotkeyPopups = new BooleanSetting("Hotkey Popups", true);
    public static final BooleanSetting useCustomColor = new BooleanSetting("Use Custom Color", false);
    // Defaults match the LAVENDER preset so the first enable doesn't jar.
    public static final NumberSetting customHue = new NumberSetting("Hue", 269, 0, 360, 1);
    public static final NumberSetting customSaturation = new NumberSetting("Saturation", 21, 0, 100, 1);
    public static final NumberSetting customValue = new NumberSetting("Value", 92, 0, 100, 1);
    public static final NumberSetting menuAnimationSpeed = new NumberSetting("Menu Animation Speed", 1.0, 0.25, 4.0, 0.05);
    public static final EnumSetting<Corner> notificationCorner = new EnumSetting<>("Notification Corner", Corner.BOTTOM_RIGHT);
    public static final NumberSetting notificationLifetime = new NumberSetting("Notification Lifetime", 1.6, 0.5, 10.0, 0.1);
    public static final EnumSetting<HudMode> hudMode = new EnumSetting<>("HUD Overlay", HudMode.OFF);
    public static final NumberSetting autosaveInterval = new NumberSetting("Autosave Interval", 60, 0, 300, 1);
    public static final BooleanSetting keybindConflictWarnings = new BooleanSetting("Keybind Conflict Warnings", true);
    public static final BooleanSetting discordPresence = new BooleanSetting("Discord Presence", true);
    public static final BooleanSetting stealthMode = new BooleanSetting("Stealth Mode", false);

    private GlobalConfig() {
    }

    /** Loads persisted client settings from disk; called once during client initialization. */
    public static void init() {
        load();
    }

    public static boolean hotkeyPopupsEnabled() {
        return hotkeyPopups.get();
    }

    /** Whether toasts are shown when a keybind toggles a module. */
    public static void setHotkeyPopupsEnabled(boolean enabled) {
        if (hotkeyPopups.get() == enabled) return;
        hotkeyPopups.set(enabled);
        save();
    }

    /** Flips the hotkey popup preference. */
    public static void toggleHotkeyPopups() {
        setHotkeyPopupsEnabled(!hotkeyPopups.get());
    }

    /** The active theme preset; used for the accent when {@link #useCustomColor} is off. */
    public static ThemeColor themeColor() {
        return themeColor;
    }

    /** Sets the active theme preset and persists the change. */
    public static void setThemeColor(ThemeColor color) {
        if (themeColor == color) return;
        themeColor = color;
        save();
    }

    /** Advances to the next {@link ThemeColor} preset, wrapping around. */
    public static void cycleThemeColor() {
        ThemeColor[] values = ThemeColor.values();
        setThemeColor(values[(themeColor.ordinal() + 1) % values.length]);
    }

    /** Whether the accent is taken from the custom HSV sliders instead of the theme preset. */
    public static boolean useCustomColor() {
        return useCustomColor.get();
    }

    /** Enables/disables the custom color override and persists the change. */
    public static void setUseCustomColor(boolean enabled) {
        if (useCustomColor.get() == enabled) return;
        useCustomColor.set(enabled);
        save();
    }

    /** Multiplier for the menu panel slide and toast slide. */
    public static double animationSpeed() {
        return menuAnimationSpeed.get();
    }

    /** Sets the menu/notification animation speed multiplier and persists the change. */
    public static void setAnimationSpeed(double speed) {
        if (menuAnimationSpeed.get() == speed) return;
        menuAnimationSpeed.set(speed);
        save();
    }

    /** The screen corner in which toasts are rendered. */
    public static Corner notificationCorner() {
        return notificationCorner.get();
    }

    /** Sets the toast corner and persists the change. */
    public static void setNotificationCorner(Corner corner) {
        if (notificationCorner.get() == corner) return;
        notificationCorner.set(corner);
        save();
    }

    /** Advances to the next {@link Corner}, wrapping around. */
    public static void cycleNotificationCorner() {
        setNotificationCorner(Corner.values()[(notificationCorner.get().ordinal() + 1) % Corner.values().length]);
    }

    /** How long a toast holds still on screen before sliding out, in seconds. */
    public static double notificationHoldSeconds() {
        return notificationLifetime.get();
    }

    /** Sets the toast hold duration in seconds and persists the change. */
    public static void setNotificationHoldSeconds(double seconds) {
        if (notificationLifetime.get() == seconds) return;
        notificationLifetime.set(seconds);
        save();
    }

    /** The currently selected HUD overlay mode ({@link HudMode#OFF}, LEGACY or MODERN). */
    public static HudMode hudMode() {
        return hudMode.get();
    }

    /** Sets the HUD overlay mode and persists the change. */
    public static void setHudMode(HudMode mode) {
        if (hudMode.get() == mode) return;
        hudMode.set(mode);
        save();
    }

    /** Advances to the next {@link HudMode}, wrapping around. */
    public static void cycleHudMode() {
        setHudMode(HudMode.values()[(hudMode.get().ordinal() + 1) % HudMode.values().length]);
    }

    /** Seconds between automatic saves; 0 disables the periodic autosave. */
    public static double autosaveIntervalSeconds() {
        return autosaveInterval.get();
    }

    /** Sets the autosave interval in seconds (0 disables periodic autosaving) and persists it. */
    public static void setAutosaveInterval(double seconds) {
        if (autosaveInterval.get() == seconds) return;
        autosaveInterval.set(seconds);
        save();
    }

    /** Whether binding a combo that collides with an existing bind warns via toast + red highlight. */
    public static boolean keybindConflictWarningsEnabled() {
        return keybindConflictWarnings.get();
    }

    /** Enables/disables keybind conflict warnings and persists the change. */
    public static void setKeybindConflictWarningsEnabled(boolean enabled) {
        if (keybindConflictWarnings.get() == enabled) return;
        keybindConflictWarnings.set(enabled);
        save();
    }

    /** Flips the keybind conflict warning preference. */
    public static void toggleKeybindConflictWarnings() {
        setKeybindConflictWarningsEnabled(!keybindConflictWarnings.get());
    }

    /** Whether Discord Rich Presence is shown instead of the default Minecraft activity. */
    public static boolean discordPresence() {
        return discordPresence.get();
    }

    /**
     * Enables/disables Discord Rich Presence. Toggling has side effects on the live
     * IPC connection, so changes go through {@link DiscordPresenceManager} via this
     * single choke point instead of the raw setting.
     */
    public static void setDiscordPresence(boolean enabled) {
        if (discordPresence.get() == enabled) return;
        discordPresence.set(enabled);
        save();
        if (enabled) {
            DiscordPresenceManager.enable();
        } else {
            DiscordPresenceManager.disable();
        }
    }

    /** Flips Discord Rich Presence on/off. */
    public static void toggleDiscordPresence() {
        setDiscordPresence(!discordPresence.get());
    }

    /** Whether the Stealth Mode panic state is currently active. */
    public static boolean stealthMode() {
        return stealthMode.get();
    }

    /**
     * Enables/disables Stealth Mode. The snapshot/restore of every module's state is
     * owned by {@link StealthManager}; this setter is the single entry point both the
     * config screen and the keybind use, and it saves on change like every other setting.
     */
    public static void setStealthMode(boolean active) {
        StealthManager.setActive(active);
    }

    /** Flips Stealth Mode on/off via {@link StealthManager}. */
    public static void toggleStealthMode() {
        StealthManager.setActive(!stealthMode.get());
    }

    /** Effective accent: the custom color when enabled, otherwise the theme preset. */
    public static int accent() {
        return PartyManager.rainbowAccent(useCustomColor.get() ? customAccent() : themeColor.accent());
    }

    /** Partially transparent accent, e.g. the click-gui scroll bar. */
    public static int accentDim() {
        return PartyManager.rainbowAccent(useCustomColor.get()
                ? (0x66000000 | (customAccent() & 0x00FFFFFF))
                : themeColor.accentDim());
    }

    /** Faint accent wash behind enabled/highlighted rows. */
    public static int enabledBg() {
        return PartyManager.rainbowAccent(useCustomColor.get()
                ? (0x40000000 | (customAccent() & 0x00FFFFFF))
                : themeColor.enabledBg());
    }

    /** Derived from the Hue/Saturation/Value sliders via HSV-to-RGB. */
    public static int customAccent() {
        return 0xFF000000 | hsvToRgb(
                (float) (customHue.get() / 360.0),
                (float) (customSaturation.get() / 100.0),
                (float) (customValue.get() / 100.0));
    }

    /** Converts a normalized HSV triplet (h in [0,1), s and v in [0,1]) to an RGB int. */
    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        switch ((int) Math.floor(h * 6) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int red = (int) Math.round((r + m) * 255);
        int green = (int) Math.round((g + m) * 255);
        int blue = (int) Math.round((b + m) * 255);
        return (red << 16) | (green << 8) | blue;
    }

    /** Reads {@code client.json} into the static settings, tolerating stale or missing keys. */
    private static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject json = root.getAsJsonObject();

            if (json.has("hotkeyPopupsEnabled")) {
                hotkeyPopups.set(json.get("hotkeyPopupsEnabled").getAsBoolean());
            }
            if (json.has("themeColor")) {
                try {
                    themeColor = Enum.valueOf(ThemeColor.class, json.get("themeColor").getAsString());
                } catch (IllegalArgumentException e) {
                    // Unknown value left over from an older config; keep the default.
                    Zephyr.LOGGER.warn("[Zephyr] Unknown theme color '{}', keeping default.",
                            json.get("themeColor").getAsString());
                }
            }
            if (json.has("useCustomColor")) {
                useCustomColor.set(json.get("useCustomColor").getAsBoolean());
            }
            readNumber(json, "customHue", customHue);
            readNumber(json, "customSaturation", customSaturation);
            readNumber(json, "customValue", customValue);
            readNumber(json, "menuAnimationSpeed", menuAnimationSpeed);
            readEnum(json, "notificationCorner", notificationCorner);
            readNumber(json, "notificationLifetime", notificationLifetime);
            readEnum(json, "hudMode", hudMode);
            readNumber(json, "autosaveInterval", autosaveInterval);
            if (json.has("keybindConflictWarnings")) {
                keybindConflictWarnings.set(json.get("keybindConflictWarnings").getAsBoolean());
            }
            if (json.has("discordPresence")) {
                discordPresence.set(json.get("discordPresence").getAsBoolean());
            }
            if (json.has("stealthMode")) {
                stealthMode.set(json.get("stealthMode").getAsBoolean());
            }
            // Stealth is a panic state, not a preference: if it was left on at shutdown
            // there is no live snapshot to restore from, so start fresh next session.
            if (stealthMode.get()) {
                stealthMode.set(false);
            }
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load client config, falling back to defaults.", e);
        }
    }

    /** Restores a numeric setting from {@code key}, keeping the default on non-numeric values. */
    private static void readNumber(JsonObject json, String key, NumberSetting setting) {
        if (!json.has(key)) return;
        try {
            setting.set(json.get(key).getAsDouble());
        } catch (RuntimeException e) {
            // Stale/non-numeric value left over from an older config; keep the default.
            Zephyr.LOGGER.warn("[Zephyr] Bad value '{}' for '{}', keeping default.", json.get(key), key);
        }
    }

    /** Restores an enum setting from {@code key}, keeping the default on unknown constants. */
    private static <T extends Enum<T>> void readEnum(JsonObject json, String key, EnumSetting<T> setting) {
        if (!json.has(key)) return;
        try {
            setting.set(Enum.valueOf(setting.getEnumType(), json.get(key).getAsString()));
        } catch (IllegalArgumentException e) {
            // Unknown value left over from an older config; keep the default.
            Zephyr.LOGGER.warn("[Zephyr] Unknown '{}' value '{}', keeping default.", key, json.get(key).getAsString());
        }
    }

    /** Writes every global setting to {@code client.json}. */
    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("hotkeyPopupsEnabled", hotkeyPopups.get());
        root.addProperty("themeColor", themeColor.name());
        root.addProperty("useCustomColor", useCustomColor.get());
        root.addProperty("customHue", customHue.get());
        root.addProperty("customSaturation", customSaturation.get());
        root.addProperty("customValue", customValue.get());
        root.addProperty("menuAnimationSpeed", menuAnimationSpeed.get());
        root.addProperty("notificationCorner", notificationCorner.get().name());
        root.addProperty("notificationLifetime", notificationLifetime.get());
        root.addProperty("hudMode", hudMode.get().name());
        root.addProperty("autosaveInterval", autosaveInterval.get());
        root.addProperty("keybindConflictWarnings", keybindConflictWarnings.get());
        root.addProperty("discordPresence", discordPresence.get());
        root.addProperty("stealthMode", stealthMode.get());

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save client config.", e);
        }
    }
}
