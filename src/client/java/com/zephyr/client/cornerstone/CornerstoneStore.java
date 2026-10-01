package com.zephyr.client.cornerstone;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.zephyr.Zephyr;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent store for cornerstone saves. Lives in
 * {@code config/zephyr/cornerstone.json} so it shares Zephyr's config dir
 * (instead of cornerstone's standalone {@code config/cornerstone.json}).
 */
public final class CornerstoneStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("cornerstone.json");

    static final class Data {
        int commandsPerTick = 5;
        Map<String, SavedRegion> saves = new LinkedHashMap<>();
    }

    private static Data data = new Data();

    private CornerstoneStore() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                if (loaded.saves == null) loaded.saves = new LinkedHashMap<>();
                if (loaded.commandsPerTick < 1) loaded.commandsPerTick = 5;
                data = loaded;
            }
        } catch (IOException | JsonParseException e) {
            Zephyr.LOGGER.error("[Zephyr] Failed to read {}", FILE, e);
        }
    }

    private static boolean write() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
            return true;
        } catch (IOException e) {
            Zephyr.LOGGER.error("[Zephyr] Failed to write {}", FILE, e);
            return false;
        }
    }

    public static boolean put(String name, SavedRegion region) {
        data.saves.put(name, region);
        return write();
    }

    public static SavedRegion get(String name) {
        return data.saves.get(name);
    }

    public static boolean remove(String name) {
        boolean existed = data.saves.remove(name) != null;
        if (existed) write();
        return existed;
    }

    public static List<String> names() {
        return new ArrayList<>(data.saves.keySet());
    }

    public static int commandsPerTick() {
        return Math.max(1, data.commandsPerTick);
    }
}
