package com.zephyr.client.configplusgui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.module.ModuleManager;
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

/**
 * Manages named module-state profiles persisted to {@code .minecraft/config/zephyr/profiles.json}.
 * A profile is a full snapshot of every module's enabled flag and setting values, produced and
 * re-applied via {@link ConfigManager#writeModuleStates} / {@link ConfigManager#applyModuleStates}
 * so profile switching reuses the exact same (de)serialization as the module config. Exactly one
 * profile ("Default" until changed) is active at any time; the manager keeps that active profile's
 * snapshot fresh as modules change.
 */
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

    /** Loads persisted profiles; creates the "Default" profile if none exist. */
    public static void init() {
        load();
        if (PROFILES.isEmpty()) {
            PROFILES.put(DEFAULT_PROFILE, allOffSnapshot());
            activeProfile = DEFAULT_PROFILE;
            save();
        }
    }

    /** Builds a snapshot of the current module state with every module disabled. */
    private static JsonObject allOffSnapshot() {
        JsonObject snapshot = ConfigManager.writeModuleStates(ModuleManager.getModules());
        for (String moduleName : snapshot.keySet()) {
            snapshot.getAsJsonObject(moduleName).addProperty("enabled", false);
        }
        return snapshot;
    }

    /** Returns the names of all stored profiles, in creation order. */
    public static List<String> getProfileNames() {
        return new ArrayList<>(PROFILES.keySet());
    }

    /** Returns the name of the currently active profile. */
    public static String getActiveProfile() {
        return activeProfile;
    }

    /**
     * Creates a new profile snapshotting the current module state and makes it active.
     *
     * @param name the profile name; must be non-blank and not already in use
     * @return true if the profile was created, false if the name was invalid or taken
     */
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

    /**
     * Saves the current module state into the active profile, then applies the named
     * profile's snapshot to the live modules (firing onEnable/onDisable side effects)
     * and switches the active profile.
     *
     * @return true if the profile existed and was applied
     */
    public static boolean applyProfile(String name) {
        if (!PROFILES.containsKey(name)) return false;

        captureActiveProfile();

        JsonObject snapshot = PROFILES.get(name);
        ConfigManager.applyModuleStates(snapshot, ModuleManager.getModules(), true);
        activeProfile = name;
        save();
        return true;
    }

    /**
     * Renames an existing profile, preserving its snapshot and adjusting the active
     * profile reference if it was the renamed one.
     *
     * @return true if the rename succeeded, false if the target name is invalid/taken
     */
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

    /**
     * Deletes a profile; the last remaining profile cannot be removed. If the active
     * profile was deleted, another profile becomes active.
     *
     * @return true if the profile was deleted
     */
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

    /** Re-snapshots the active profile from the current live module state. */
    public static void captureActiveProfile() {
        if (!PROFILES.containsKey(activeProfile)) return;
        PROFILES.put(activeProfile, ConfigManager.writeModuleStates(ModuleManager.getModules()));
        save();
    }

    /** Reads {@code profiles.json}, restoring all profiles and the active profile name. */
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

    /** Writes all profiles and the active profile name to {@code profiles.json}. */
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