package com.zephyr.client.configplusgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KeybindManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("keybinds.json");

    public enum SystemAction {
        OPEN_MENU("Open Menu", new Keybind(GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_ENTER, Keybind.UNSET)),
        CYCLE_SCREEN("Cycle Screen", new Keybind(GLFW.GLFW_KEY_TAB, Keybind.UNSET, Keybind.UNSET));

        public final String label;
        public final Keybind defaultBind;

        SystemAction(String label, Keybind defaultBind) {
            this.label = label;
            this.defaultBind = defaultBind;
        }
    }

    private static final Map<String, Keybind> MODULE_BINDS = new LinkedHashMap<>();
    private static final Map<SystemAction, Keybind> SYSTEM_BINDS = new EnumMap<>(SystemAction.class);

    private static final Map<String, Boolean> MODULE_WAS_DOWN = new HashMap<>();
    private static final Map<SystemAction, Boolean> SYSTEM_WAS_DOWN = new EnumMap<>(SystemAction.class);

    private KeybindManager() {
    }

    public static void init() {
        load();
    }

    public static Keybind get(Module module) {
        return MODULE_BINDS.getOrDefault(module.getName(), Keybind.NONE);
    }

    public static void set(Module module, Keybind bind) {
        MODULE_BINDS.put(module.getName(), bind);
        save();
    }

    public static void clear(Module module) {
        set(module, Keybind.NONE);
    }

    public static Keybind get(SystemAction action) {
        return SYSTEM_BINDS.getOrDefault(action, action.defaultBind);
    }

    public static void set(SystemAction action, Keybind bind) {
        SYSTEM_BINDS.put(action, bind);
        save();
    }

    public static void resetToDefault(SystemAction action) {
        SYSTEM_BINDS.remove(action);
        save();
    }

    public static void tick(Minecraft client) {
        Screen screen = client.gui.screen();
        boolean typing = screen != null && screen.getFocused() instanceof EditBox;
        boolean capturingBind = screen instanceof KeybindGuiScreen keybindGuiScreen && keybindGuiScreen.isCapturing();
        boolean suppressed = typing || capturingBind;

        if (!suppressed) {
            tickOpenMenu(client, screen);
        }

        if (screen instanceof ZephyrScreen zephyrScreen && !capturingBind) {
            tickCycleScreen(client, zephyrScreen);
        } else {
            SYSTEM_WAS_DOWN.put(SystemAction.CYCLE_SCREEN, false);
        }

        if (!suppressed) {
            tickModuleBinds(client);
        }
    }

    private static void tickOpenMenu(Minecraft client, Screen screen) {
        boolean down = isDown(client, get(SystemAction.OPEN_MENU));
        boolean wasDown = SYSTEM_WAS_DOWN.getOrDefault(SystemAction.OPEN_MENU, false);

        if (down && !wasDown) {
            if (screen == null) {
                client.gui.setScreen(new ClickGuiScreen());
            } else if (screen instanceof ZephyrScreen zephyrScreen) {
                zephyrScreen.onClose(); // chains to Screen#onClose(), which sets the screen to null
            }
        }
        SYSTEM_WAS_DOWN.put(SystemAction.OPEN_MENU, down);
    }

    private static void tickCycleScreen(Minecraft client, ZephyrScreen current) {
        boolean down = isDown(client, get(SystemAction.CYCLE_SCREEN));
        boolean wasDown = SYSTEM_WAS_DOWN.getOrDefault(SystemAction.CYCLE_SCREEN, false);

        if (down && !wasDown) {
            client.gui.setScreen(current.next());
        }
        SYSTEM_WAS_DOWN.put(SystemAction.CYCLE_SCREEN, down);
    }

    private static void tickModuleBinds(Minecraft client) {
        for (Module module : ModuleManager.getModules()) {
            Keybind bind = get(module);
            if (!bind.isSet()) continue;

            boolean down = isDown(client, bind);
            boolean wasDown = MODULE_WAS_DOWN.getOrDefault(module.getName(), false);

            if (down && !wasDown) {
                module.toggle();
            }
            MODULE_WAS_DOWN.put(module.getName(), down);
        }
    }

    private static boolean isDown(Minecraft client, Keybind bind) {
        if (!bind.isSet()) return false;

        long windowHandle = client.getWindow().handle();
        for (int key : bind.keys()) {
            if (key == Keybind.UNSET) continue;
            if (GLFW.glfwGetKey(windowHandle, key) != GLFW.GLFW_PRESS) return false;
        }
        return true;
    }

    private static void load() {
        MODULE_BINDS.clear();
        SYSTEM_BINDS.clear();

        if (!Files.exists(CONFIG_PATH)) return;

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject json = root.getAsJsonObject();

            if (json.has("actions")) {
                JsonObject actions = json.getAsJsonObject("actions");
                for (SystemAction action : SystemAction.values()) {
                    if (actions.has(action.name())) {
                        SYSTEM_BINDS.put(action, readKeybind(actions.getAsJsonArray(action.name())));
                    }
                }
            }

            if (json.has("modules")) {
                JsonObject modules = json.getAsJsonObject("modules");
                for (String moduleName : modules.keySet()) {
                    MODULE_BINDS.put(moduleName, readKeybind(modules.getAsJsonArray(moduleName)));
                }
            }
        } catch (IOException | RuntimeException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to load keybinds, falling back to defaults.", e);
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();

        JsonObject actions = new JsonObject();
        for (Map.Entry<SystemAction, Keybind> entry : SYSTEM_BINDS.entrySet()) {
            actions.add(entry.getKey().name(), writeKeybind(entry.getValue()));
        }
        root.add("actions", actions);

        JsonObject modules = new JsonObject();
        for (Map.Entry<String, Keybind> entry : MODULE_BINDS.entrySet()) {
            modules.add(entry.getKey(), writeKeybind(entry.getValue()));
        }
        root.add("modules", modules);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Zephyr.LOGGER.warn("[Zephyr] Failed to save keybinds.", e);
        }
    }

    private static Keybind readKeybind(JsonArray array) {
        int[] keys = new int[Keybind.MAX_KEYS];
        java.util.Arrays.fill(keys, Keybind.UNSET);
        for (int i = 0; i < Math.min(Keybind.MAX_KEYS, array.size()); i++) {
            keys[i] = array.get(i).getAsInt();
        }
        return new Keybind(keys);
    }

    private static JsonArray writeKeybind(Keybind bind) {
        JsonArray array = new JsonArray();
        for (int key : bind.keys()) {
            array.add(key);
        }
        return array;
    }
}