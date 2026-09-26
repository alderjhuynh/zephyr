package com.zephyr.client.notebook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.Collections;
import java.util.List;

/**
 * Client-side persistent storage for the notebook feature.
 * Data is kept in {@code config/zephyr/notebook.json} as
 * {@code {"pages":["...","..."]}} and never sent to the server.
 */
public final class NotebookStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("notebook.json");

    private static List<String> pages = new ArrayList<>();
    private static boolean loaded = false;

    private NotebookStorage() {
    }

    public static synchronized void loadIfNeeded() {
        if (loaded) return;
        load();
        loaded = true;
    }

    public static synchronized List<String> getPages() {
        loadIfNeeded();
        return new ArrayList<>(pages);
    }

    public static synchronized void savePages(List<String> newPages) {
        // Defensive copy, trim trailing empty pages like vanilla BookEditScreen
        List<String> copy = new ArrayList<>(newPages);
        // Keep at least one page
        if (copy.isEmpty()) {
            copy.add("");
        }
        pages = new ArrayList<>(copy);
        save();
    }

    public static synchronized void clear() {
        pages = new ArrayList<>();
        pages.add("");
        save();
    }

    private static void load() {
        if (!Files.exists(PATH)) {
            pages = new ArrayList<>();
            pages.add("");
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                pages = new ArrayList<>();
                pages.add("");
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            JsonArray arr = obj.has("pages") && obj.get("pages").isJsonArray() ? obj.getAsJsonArray("pages") : null;
            if (arr == null || arr.isEmpty()) {
                pages = new ArrayList<>();
                pages.add("");
                return;
            }
            List<String> loadedPages = new ArrayList<>();
            for (JsonElement e : arr) {
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                    loadedPages.add(e.getAsString());
                }
            }
            if (loadedPages.isEmpty()) {
                loadedPages.add("");
            }
            // Vanilla limits to 100 pages
            if (loadedPages.size() > 100) {
                loadedPages = loadedPages.subList(0, 100);
            }
            pages = new ArrayList<>(loadedPages);
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load notebook, using empty notebook.", e);
            pages = new ArrayList<>();
            pages.add("");
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String p : pages) {
            arr.add(p);
        }
        root.add("pages", arr);
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save notebook.", e);
        }
    }
}
