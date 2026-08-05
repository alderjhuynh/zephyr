package com.zephyr.client.configplusgui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.hud.Corner;
import com.zephyr.client.configplusgui.hud.HudMode;
import com.zephyr.client.configplusgui.hud.ThemeColor;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public static final BooleanSetting stealthMode = new BooleanSetting("Stealth Mode", false);

    private GlobalConfig() {
    }

    public static void init() {
        load();
    }

    public static boolean hotkeyPopupsEnabled() {
        return hotkeyPopups.get();
    }

    public static void setHotkeyPopupsEnabled(boolean enabled) {
        if (hotkeyPopups.get() == enabled) return;
        hotkeyPopups.set(enabled);
        save();
    }

    public static void toggleHotkeyPopups() {
        setHotkeyPopupsEnabled(!hotkeyPopups.get());
    }

    public static ThemeColor themeColor() {
        return themeColor;
    }

    public static void setThemeColor(ThemeColor color) {
        if (themeColor == color) return;
        themeColor = color;
        save();
    }

    public static void cycleThemeColor() {
        ThemeColor[] values = ThemeColor.values();
        setThemeColor(values[(themeColor.ordinal() + 1) % values.length]);
    }

    public static boolean useCustomColor() {
        return useCustomColor.get();
    }

    public static void setUseCustomColor(boolean enabled) {
        if (useCustomColor.get() == enabled) return;
        useCustomColor.set(enabled);
        save();
    }

    /** Multiplier for the menu panel slide and toast slide. */
    public static double animationSpeed() {
        return menuAnimationSpeed.get();
    }

    public static void setAnimationSpeed(double speed) {
        if (menuAnimationSpeed.get() == speed) return;
        menuAnimationSpeed.set(speed);
        save();
    }

    public static Corner notificationCorner() {
        return notificationCorner.get();
    }

    public static void setNotificationCorner(Corner corner) {
        if (notificationCorner.get() == corner) return;
        notificationCorner.set(corner);
        save();
    }

    public static void cycleNotificationCorner() {
        setNotificationCorner(Corner.values()[(notificationCorner.get().ordinal() + 1) % Corner.values().length]);
    }

    /** How long a toast holds still on screen before sliding out, in seconds. */
    public static double notificationHoldSeconds() {
        return notificationLifetime.get();
    }

    public static void setNotificationHoldSeconds(double seconds) {
        if (notificationLifetime.get() == seconds) return;
        notificationLifetime.set(seconds);
        save();
    }

    public static HudMode hudMode() {
        return hudMode.get();
    }

    public static void setHudMode(HudMode mode) {
        if (hudMode.get() == mode) return;
        hudMode.set(mode);
        save();
    }

    public static void cycleHudMode() {
        setHudMode(HudMode.values()[(hudMode.get().ordinal() + 1) % HudMode.values().length]);
    }

    /** Seconds between automatic saves; 0 disables the periodic autosave. */
    public static double autosaveIntervalSeconds() {
        return autosaveInterval.get();
    }

    public static void setAutosaveInterval(double seconds) {
        if (autosaveInterval.get() == seconds) return;
        autosaveInterval.set(seconds);
        save();
    }

    /** Whether binding a combo that collides with an existing bind warns via toast + red highlight. */
    public static boolean keybindConflictWarningsEnabled() {
        return keybindConflictWarnings.get();
    }

    public static void setKeybindConflictWarningsEnabled(boolean enabled) {
        if (keybindConflictWarnings.get() == enabled) return;
        keybindConflictWarnings.set(enabled);
        save();
    }

    public static void toggleKeybindConflictWarnings() {
        setKeybindConflictWarningsEnabled(!keybindConflictWarnings.get());
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

    public static void toggleStealthMode() {
        StealthManager.setActive(!stealthMode.get());
    }

    /** Effective accent: the custom color when enabled, otherwise the theme preset. */
    public static int accent() {
        return useCustomColor.get() ? customAccent() : themeColor.accent();
    }

    /** Partially transparent accent, e.g. the click-gui scroll bar. */
    public static int accentDim() {
        return useCustomColor.get()
                ? (0x66000000 | (customAccent() & 0x00FFFFFF))
                : themeColor.accentDim();
    }

    /** Faint accent wash behind enabled/highlighted rows. */
    public static int enabledBg() {
        return useCustomColor.get()
                ? (0x40000000 | (customAccent() & 0x00FFFFFF))
                : themeColor.enabledBg();
    }

    /** Derived from the Hue/Saturation/Value sliders via HSV-to-RGB. */
    public static int customAccent() {
        return 0xFF000000 | hsvToRgb(
                (float) (customHue.get() / 360.0),
                (float) (customSaturation.get() / 100.0),
                (float) (customValue.get() / 100.0));
    }

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

    private static void readNumber(JsonObject json, String key, NumberSetting setting) {
        if (!json.has(key)) return;
        try {
            setting.set(json.get(key).getAsDouble());
        } catch (RuntimeException e) {
            // Stale/non-numeric value left over from an older config; keep the default.
            Zephyr.LOGGER.warn("[Zephyr] Bad value '{}' for '{}', keeping default.", json.get(key), key);
        }
    }

    private static <T extends Enum<T>> void readEnum(JsonObject json, String key, EnumSetting<T> setting) {
        if (!json.has(key)) return;
        try {
            setting.set(Enum.valueOf(setting.getEnumType(), json.get(key).getAsString()));
        } catch (IllegalArgumentException e) {
            // Unknown value left over from an older config; keep the default.
            Zephyr.LOGGER.warn("[Zephyr] Unknown '{}' value '{}', keeping default.", key, json.get(key).getAsString());
        }
    }

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
