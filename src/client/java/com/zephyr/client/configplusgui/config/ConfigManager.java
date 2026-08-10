package com.zephyr.client.configplusgui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.Setting;
import com.zephyr.client.configplusgui.setting.StringSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Persists every module's enabled state and setting values to
 * {@code .minecraft/config/zephyr/modules.json}. The read/write helpers for a single
 * module's state are package-visible so {@link ProfileManager} can reuse the exact
 * same (de)serialization for its own named snapshots instead of duplicating it.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("modules.json");

    private ConfigManager() {
    }

    public static void load(List<Module> modules) {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            applyModuleStates(root.getAsJsonObject(), modules, false);
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load module config, falling back to defaults.", e);
        }
    }

    public static void save(List<Module> modules) {
        JsonObject root = writeModuleStates(modules);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save module config.", e);
        }
    }

    /**
     * Applies a {@code {moduleName: {enabled, settings}}} snapshot (as produced by
     * {@link #writeModuleStates}) onto the given live modules. Modules or settings not
     * present in {@code moduleStates} are left untouched.
     *
     * @param live if true, changes to a module's enabled flag go through
     *             {@link Module#setEnabled(boolean)} so {@code onEnable()}/{@code onDisable()}
     *             actually fire - use this for a runtime profile switch. If false, uses
     *             {@link Module#setEnabledSilently(boolean)} - use this for the initial
     *             config load at startup, before the game is ready for those side effects.
     */
    static void applyModuleStates(JsonObject moduleStates, List<Module> modules, boolean live) {
        for (Module module : modules) {
            if (!moduleStates.has(module.getName())) continue;
            JsonObject data = moduleStates.getAsJsonObject(module.getName());

            if (data.has("enabled")) {
                boolean enabled = data.get("enabled").getAsBoolean();
                if (live) {
                    module.setEnabled(enabled);
                } else {
                    module.setEnabledSilently(enabled);
                }
            }

            if (data.has("settings")) {
                JsonObject settingsJson = data.getAsJsonObject("settings");
                for (Setting<?> setting : module.getSettings()) {
                    if (!settingsJson.has(setting.getName())) continue;
                    applySettingValue(setting, settingsJson.get(setting.getName()));
                }
            }
        }
    }

    /** Snapshots every module's enabled state and setting values into a {@code {moduleName: {enabled, settings}}} object. */
    static JsonObject writeModuleStates(List<Module> modules) {
        JsonObject root = new JsonObject();

        for (Module module : modules) {
            JsonObject data = new JsonObject();
            data.addProperty("enabled", module.isEnabled());

            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                writeSettingValue(settingsJson, setting);
            }
            data.add("settings", settingsJson);

            root.add(module.getName(), data);
        }

        return root;
    }

    private static void applySettingValue(Setting<?> setting, JsonElement element) {
        if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.set(element.getAsBoolean());
        } else if (setting instanceof NumberSetting numberSetting) {
            numberSetting.set(element.getAsDouble());
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            applyEnumValue(enumSetting, element.getAsString());
        } else if (setting instanceof StringSetting stringSetting) {
            stringSetting.set(element.getAsString());
        } else if (setting instanceof ListSetting listSetting) {
            applyListValue(listSetting, element.getAsJsonArray());
        }
    }

    private static void applyListValue(ListSetting setting, JsonArray array) {
        setting.clear();
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            String blockName = entry.has("block") ? entry.get("block").getAsString() : "";
            String color = entry.has("color") ? entry.get("color").getAsString() : "";
            setting.add(blockName, color);
        }
    }

    private static <T extends Enum<T>> void applyEnumValue(EnumSetting<T> enumSetting, String name) {
        try {
            enumSetting.set(Enum.valueOf(enumSetting.getEnumType(), name));
        } catch (IllegalArgumentException e) {
            // Stale/unknown constant left over from an older config; keep the default.
            Zephyr.LOGGER.warn("[Zephyr] Unknown value '{}' for setting '{}', keeping default.",
                    name, enumSetting.getName());
        }
    }

    private static void writeSettingValue(JsonObject settingsJson, Setting<?> setting) {
        if (setting instanceof BooleanSetting booleanSetting) {
            settingsJson.addProperty(setting.getName(), booleanSetting.get());
        } else if (setting instanceof NumberSetting numberSetting) {
            settingsJson.addProperty(setting.getName(), numberSetting.get());
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            settingsJson.addProperty(setting.getName(), enumSetting.get().name());
        } else if (setting instanceof StringSetting stringSetting) {
            settingsJson.addProperty(setting.getName(), stringSetting.get());
        } else if (setting instanceof ListSetting listSetting) {
            JsonArray array = new JsonArray();
            for (ListSetting.ListEntry entry : listSetting.get()) {
                JsonObject object = new JsonObject();
                object.addProperty("block", entry.blockName());
                object.addProperty("color", entry.color());
                array.add(object);
            }
            settingsJson.add(setting.getName(), array);
        }
    }
}