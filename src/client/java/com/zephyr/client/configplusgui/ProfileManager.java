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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfileManager {
    private static final String DEFAULT_PROFILE = "Default";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("profiles.json");

    private static final Map<String, JsonObject> PROFILES = new LinkedHashMap<>();
    private static String activeProfile = DEFAULT_PROFILE;

    private ProfileManager() {
    }

    public static void init() {
        load();
        if (PROFILES.isEmpty()) {
            PROFILES.put(DEFAULT_PROFILE, allOffSnapshot());
            activeProfile = DEFAULT_PROFILE;
            save();
        }
    }

    private static JsonObject allOffSnapshot() {
        JsonObject snapshot = ConfigManager.writeModuleStates(ModuleManager.getModules());
        for (String moduleName : snapshot.keySet()) {
            snapshot.getAsJsonObject(moduleName).addProperty("enabled", false);
        }
        return snapshot;
    }

    public static List<String> getProfileNames() {
        return new ArrayList<>(PROFILES.keySet());
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static boolean createProfile(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || PROFILES.containsKey(trimmed)) {
            return false;
        }

        PROFILES.put(trimmed, ConfigManager.writeModuleStates(ModuleManager.getModules()));
        activeProfile = trimmed;
        save();
        return true;
    }

    public static boolean applyProfile(String name) {
        if (!PROFILES.containsKey(name)) return false;

        captureActiveProfile();

        JsonObject snapshot = PROFILES.get(name);
        ConfigManager.applyModuleStates(snapshot, ModuleManager.getModules(), true);
        activeProfile = name;
        save();
        return true;
    }

    public static boolean renameProfile(String oldName, String newName) {
        String trimmed = newName == null ? "" : newName.trim();
        if (!PROFILES.containsKey(oldName) || trimmed.isEmpty() || PROFILES.containsKey(trimmed)) {
            return false;
        }

        Map<String, JsonObject> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, JsonObject> entry : PROFILES.entrySet()) {
            if (entry.getKey().equals(oldName)) {
                rebuilt.put(trimmed, entry.getValue());
            } else {
                rebuilt.put(entry.getKey(), entry.getValue());
            }
        }
        PROFILES.clear();
        PROFILES.putAll(rebuilt);

        if (activeProfile.equals(oldName)) {
            activeProfile = trimmed;
        }
        save();
        return true;
    }

    public static boolean deleteProfile(String name) {
        if (!PROFILES.containsKey(name) || PROFILES.size() <= 1) {
            return false;
        }

        PROFILES.remove(name);
        if (activeProfile.equals(name)) {
            activeProfile = PROFILES.keySet().iterator().next();
        }
        save();
        return true;
    }

    public static void captureActiveProfile() {
        if (!PROFILES.containsKey(activeProfile)) return;
        PROFILES.put(activeProfile, ConfigManager.writeModuleStates(ModuleManager.getModules()));
        save();
    }

    private static void load() {
        PROFILES.clear();
        if (!Files.exists(CONFIG_PATH)) return;

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject json = root.getAsJsonObject();

            if (json.has("profiles")) {
                JsonObject profiles = json.getAsJsonObject("profiles");
                for (String name : profiles.keySet()) {
                    PROFILES.put(name, profiles.getAsJsonObject(name));
                }
            }
            if (json.has("active") && PROFILES.containsKey(json.get("active").getAsString())) {
                activeProfile = json.get("active").getAsString();
            } else if (!PROFILES.isEmpty()) {
                activeProfile = PROFILES.keySet().iterator().next();
            }
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load profiles, falling back to defaults.", e);
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("active", activeProfile);

        JsonObject profiles = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : PROFILES.entrySet()) {
            profiles.add(entry.getKey(), entry.getValue());
        }
        root.add("profiles", profiles);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save profiles.", e);
        }
    }
}