package com.zephyr.client.configplusgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
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

    private static boolean hotkeyPopupsEnabled = true;

    private GlobalConfig() {
    }

    public static void init() {
        load();
    }

    public static boolean hotkeyPopupsEnabled() {
        return hotkeyPopupsEnabled;
    }

    public static void setHotkeyPopupsEnabled(boolean enabled) {
        if (hotkeyPopupsEnabled == enabled) return;
        hotkeyPopupsEnabled = enabled;
        save();
    }

    public static void toggleHotkeyPopups() {
        setHotkeyPopupsEnabled(!hotkeyPopupsEnabled);
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
                hotkeyPopupsEnabled = json.get("hotkeyPopupsEnabled").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load client config, falling back to defaults.", e);
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("hotkeyPopupsEnabled", hotkeyPopupsEnabled);

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