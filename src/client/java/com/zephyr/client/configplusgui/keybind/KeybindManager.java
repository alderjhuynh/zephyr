package com.zephyr.client.configplusgui.keybind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zephyr.Zephyr;
import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import com.zephyr.client.configplusgui.screen.ClickGuiScreen;
import com.zephyr.client.configplusgui.screen.KeybindGuiScreen;
import com.zephyr.client.configplusgui.screen.ZephyrScreen;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public final class KeybindManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("zephyr")
            .resolve("keybinds.json");

    public enum SystemAction {
        OPEN_MENU("Open Menu", new Keybind(GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_ENTER, Keybind.UNSET)),
        CYCLE_SCREEN("Cycle Screen", new Keybind(GLFW.GLFW_KEY_TAB, Keybind.UNSET, Keybind.UNSET)),
        STEALTH_MODE("Stealth Mode", new Keybind(GLFW.GLFW_KEY_F6, Keybind.UNSET, Keybind.UNSET)),
        COMMAND_PREFIX("Command Prefix", new Keybind(GLFW.GLFW_KEY_PERIOD, Keybind.UNSET, Keybind.UNSET));

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
        boolean hadConflict = isModuleConflicted(module);
        MODULE_BINDS.put(module.getName(), bind);
        recomputeConflicts();
        if (GlobalConfig.keybindConflictWarningsEnabled() && !hadConflict && isModuleConflicted(module)) {
            NotificationManager.notify("Keybind conflict", true);
        }
        save();
    }

    public static void clear(Module module) {
        MODULE_BINDS.remove(module.getName());
        recomputeConflicts();
        save();
    }

    public static Keybind get(SystemAction action) {
        return SYSTEM_BINDS.getOrDefault(action, action.defaultBind);
    }

    public static void set(SystemAction action, Keybind bind) {
        boolean hadConflict = isSystemConflicted(action);
        SYSTEM_BINDS.put(action, bind);
        recomputeConflicts();
        if (GlobalConfig.keybindConflictWarningsEnabled() && !hadConflict && isSystemConflicted(action)) {
            NotificationManager.notify("Keybind conflict", true);
        }
        save();
    }

    public static void resetToDefault(SystemAction action) {
        SYSTEM_BINDS.remove(action);
        recomputeConflicts();
        save();
    }

    public static void tick(Minecraft client) {
        Screen screen = client.gui.screen();
        boolean typing = screen != null && screen.getFocused() instanceof EditBox;
        boolean capturingBind = screen instanceof KeybindGuiScreen keybindGuiScreen && keybindGuiScreen.isCapturing();
        boolean suppressed = typing || capturingBind;

        if (!suppressed) {
            tickOpenMenu(client, screen);
            tickStealthMode(client);
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

    private static void tickStealthMode(Minecraft client) {
        boolean down = isDown(client, get(SystemAction.STEALTH_MODE));
        boolean wasDown = SYSTEM_WAS_DOWN.getOrDefault(SystemAction.STEALTH_MODE, false);

        if (down && !wasDown) {
            GlobalConfig.toggleStealthMode();
        }
        SYSTEM_WAS_DOWN.put(SystemAction.STEALTH_MODE, down);
    }

    private static void tickCycleScreen(Minecraft client, ZephyrScreen current) {
        boolean down = isDown(client, get(SystemAction.CYCLE_SCREEN));
        boolean wasDown = SYSTEM_WAS_DOWN.getOrDefault(SystemAction.CYCLE_SCREEN, false);

        if (down && !wasDown) {
            // Holding Down while cycling moves forward through the hidden
            // main/???/credits cycle, holding Up moves backward through it, and
            // holding Left moves backward through the normal screens (same polling
            // pattern as isDown()); the screen resolves the Tab-with-arrow target
            // and animation direction itself.
            long windowHandle = client.getWindow().handle();
            boolean holdingDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
            boolean holdingUp = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
            boolean holdingLeft = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
            client.gui.setScreen(current.advance(holdingDown, holdingUp, holdingLeft));
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
                NotificationManager.notify(module.getName(), module.isEnabled());
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

    /** Re-derives every bind's {@code conflicted} flag: two binds share the same combo iff both are flagged. */
    private static void recomputeConflicts() {
        List<Keybind> all = new ArrayList<>(MODULE_BINDS.values());
        for (SystemAction action : SystemAction.values()) {
            all.add(get(action));
        }

        for (Keybind bind : all) {
            bind.setConflicted(false);
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (sameCombo(all.get(i), all.get(j))) {
                    all.get(i).setConflicted(true);
                    all.get(j).setConflicted(true);
                }
            }
        }
    }

    private static boolean isModuleConflicted(Module module) {
        Keybind bind = MODULE_BINDS.get(module.getName());
        return bind != null && bind.conflicted();
    }

    private static boolean isSystemConflicted(SystemAction action) {
        return get(action).conflicted();
    }

    /** Whether two binds resolve to the same key set, ignoring order and unset slots. */
    private static boolean sameCombo(Keybind a, Keybind b) {
        if (!a.isSet() || !b.isSet()) return false;
        int[] ka = normalizedKeys(a);
        int[] kb = normalizedKeys(b);
        return java.util.Arrays.equals(ka, kb);
    }

    private static int[] normalizedKeys(Keybind bind) {
        int[] keys = bind.keys().clone();
        int count = 0;
        for (int key : keys) {
            if (key != Keybind.UNSET) {
                keys[count++] = key;
            }
        }
        java.util.Arrays.sort(keys, 0, count);
        return java.util.Arrays.copyOf(keys, count);
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