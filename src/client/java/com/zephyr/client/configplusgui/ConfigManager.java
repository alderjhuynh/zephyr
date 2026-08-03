package com.zephyr.client.configplusgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.BooleanSetting;
import com.zephyr.client.configplusgui.EnumSetting;
import com.zephyr.client.configplusgui.NumberSetting;
import com.zephyr.client.configplusgui.Setting;
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
 * {@code .minecraft/config/zephyr/modules.json}.
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
            JsonObject moduleStates = root.getAsJsonObject();

            for (Module module : modules) {
                if (!moduleStates.has(module.getName())) continue;
                JsonObject data = moduleStates.getAsJsonObject(module.getName());

                if (data.has("enabled")) {
                    module.setEnabledSilently(data.get("enabled").getAsBoolean());
                }

                if (data.has("settings")) {
                    JsonObject settingsJson = data.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!settingsJson.has(setting.getName())) continue;
                        applySettingValue(setting, settingsJson.get(setting.getName()));
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load module config, falling back to defaults.", e);
        }
    }

    public static void save(List<Module> modules) {
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

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save module config.", e);
        }
    }

    private static void applySettingValue(Setting<?> setting, JsonElement element) {
        if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.set(element.getAsBoolean());
        } else if (setting instanceof NumberSetting numberSetting) {
            numberSetting.set(element.getAsDouble());
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            applyEnumValue(enumSetting, element.getAsString());
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
        }
    }
}