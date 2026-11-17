package com.zephyr.client.api;

import com.zephyr.client.MillisScheduler;
import com.zephyr.client.TickScheduler;
import com.zephyr.client.commands.Command;
import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import com.zephyr.client.configplusgui.screen.ClickGuiScreen;
import com.zephyr.client.configplusgui.screen.ZephyrScreen;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.Setting;
import com.zephyr.client.configplusgui.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Supplier;

/**
 * The public entry point for Zephyr addons. Everything an addon needs - registering
 * modules, categories, settings, commands and screens, controlling built-in modules,
 * and reacting to events - is reachable from here, so addons never have to depend on
 * Zephyr's internal packages beyond the {@code Module}, {@code Setting} and
 * {@code Command} base classes.
 * <p>
 * Implement {@link ZephyrAddon} and declare it as a {@code "zephyr"} entrypoint in your
 * {@code fabric.mod.json} to be invoked after Zephyr has finished initializing:
 * <pre>{@code
 * "entrypoints": {
 *   "zephyr": [ "com.example.myaddon.MyAddon" ]
 * }
 * }</pre>
 * All methods are static and safe to call from the client thread.
 */
public final class ZephyrApi {
    private ZephyrApi() {
    }

    // ---------------------------------------------------------------- registration

    /**
     * Registers an addon module so it appears in the click-gui, gets persisted to
     * {@code modules.json}, receives ticks while enabled and gets a keybind slot on
     * the keybinds screen. Safe to call before or after Zephyr finishes loading;
     * persisted state is applied automatically either way.
     *
     * @param module the module singleton to register
     */
    public static void registerModule(Module module) {
        ModuleManager.registerModule(module);
    }

    /**
     * Registers (or returns the already-registered) module category with the given
     * display name. Custom categories appear in the click-gui's category dropdown and
     * as tags on module rows. Pass the returned category to your module constructors.
     *
     * @param displayName the human-readable label, e.g. "MyAddon"
     * @return the category singleton for that name
     */
    public static Category registerCategory(String displayName) {
        return Category.of(displayName);
    }

    /**
     * Registers a chat command, invocable with Zephyr's command prefix and listed in
     * tab-completion.
     *
     * @param command the command to register
     */
    public static void registerCommand(Command command) {
        CommandManager.register(command);
    }

    /**
     * Registers an addon screen into the Tab screen cycle, after the built-in screens.
     * The factory creates a fresh instance each time the cycle reaches the entry; the
     * screen is a plain {@link Screen} and should call {@link #cycleNext()} or
     * {@link #cyclePrevious()} when it wants to continue cycling.
     *
     * @param id      a unique id for the entry, e.g. your mod id
     * @param factory creates the screen instance
     */
    public static void registerCycleScreen(String id, Supplier<Screen> factory) {
        ZephyrScreen.registerCycleScreen(id, factory);
    }

    // ---------------------------------------------------------------- module access

    /**
     * Looks up a registered module by name, case-insensitively.
     *
     * @return the module, or {@code null} if none is registered
     */
    public static Module getModule(String name) {
        return ModuleManager.get(name);
    }

    /** All registered modules (built-in and addon) in registration order, read-only. */
    public static List<Module> getModules() {
        return ModuleManager.getModules();
    }

    /** All modules currently visible in the UI (respects hidden modules and Hide Detectable). */
    public static List<Module> getVisibleModules() {
        return ModuleManager.getVisibleModules();
    }

    /** Whether the named module is currently enabled; {@code false} for unknown names. */
    public static boolean isEnabled(String name) {
        Module module = ModuleManager.get(name);
        return module != null && module.isEnabled();
    }

    /** Enables or disables the named module (firing its onEnable/onDisable). No-op for unknown names. */
    public static void setEnabled(String name, boolean enabled) {
        Module module = ModuleManager.get(name);
        if (module != null) {
            module.setEnabled(enabled);
        }
    }

    /** Flips the named module between enabled and disabled. No-op for unknown names. */
    public static void toggle(String name) {
        Module module = ModuleManager.get(name);
        if (module != null) {
            module.toggle();
        }
    }

    // ---------------------------------------------------------------- settings

    /**
     * Finds a module's setting by name, case-sensitively (setting names are config keys).
     *
     * @return the setting, or {@code null} if the module or setting doesn't exist
     */
    public static Setting<?> getSetting(Module module, String settingName) {
        if (module == null) return null;
        for (Setting<?> setting : module.getSettings()) {
            if (setting.getName().equals(settingName)) {
                return setting;
            }
        }
        return null;
    }

    /**
     * Reads a setting's current value as an Object ({@code Boolean}, {@code Double},
     * {@code String}, the enum constant, or a {@code List<ListSetting.ListEntry>}).
     *
     * @return the value, or {@code null} if the module or setting doesn't exist
     */
    public static Object getSettingValue(Module module, String settingName) {
        Setting<?> setting = getSetting(module, settingName);
        return setting == null ? null : setting.get();
    }

    /**
     * Writes a setting's value with type coercion: {@code Boolean} for boolean settings,
     * any {@code Number} for number settings (clamped/snapped to the slider range), and
     * a constant {@code name()} string or enum instance for enum settings.
     *
     * @return {@code true} if the value was applied
     */
    public static boolean setSettingValue(Module module, String settingName, Object value) {
        Setting<?> setting = getSetting(module, settingName);
        if (setting == null || value == null) {
            return false;
        }
        if (setting instanceof BooleanSetting booleanSetting && value instanceof Boolean bool) {
            booleanSetting.set(bool);
            return true;
        }
        if (setting instanceof NumberSetting numberSetting && value instanceof Number number) {
            numberSetting.set(number.doubleValue());
            return true;
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            Object constant = value instanceof Enum<?> enumValue && enumSetting.getEnumType().isInstance(enumValue)
                    ? enumValue
                    : value instanceof String name ? findEnumConstant(enumSetting, name) : null;
            if (constant != null) {
                setEnumValue(enumSetting, (Enum) constant);
                return true;
            }
        }
        if (setting instanceof StringSetting stringSetting && value instanceof String string) {
            stringSetting.set(string);
            return true;
        }
        return false;
    }

    /** Appends a {@code block,color} entry to a list setting (e.g. an Xray block list). */
    public static void addListEntry(Module module, String settingName, String blockId, String color) {
        Setting<?> setting = getSetting(module, settingName);
        if (setting instanceof ListSetting listSetting) {
            listSetting.add(blockId, color);
        }
    }

    /** Removes every entry from a list setting. */
    public static void clearList(Module module, String settingName) {
        Setting<?> setting = getSetting(module, settingName);
        if (setting instanceof ListSetting listSetting) {
            listSetting.clear();
        }
    }

    /** Looks up an enum constant by name against a setting's enum type. */
    private static Object findEnumConstant(EnumSetting<?> setting, String name) {
        for (Object constant : setting.getEnumType().getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                return constant;
            }
        }
        return null;
    }

    /** Writes an enum setting's value via a raw-typed call into the setting. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumValue(EnumSetting setting, Enum constant) {
        setting.set(constant);
    }

    // ---------------------------------------------------------------- screens

    /**
     * Opens any screen, replacing whatever is currently open. Runs through the client
     * thread executor, so it is safe from any thread.
     *
     * @param screen the screen to open
     */
    public static void openScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(screen));
    }

    /** Opens Zephyr's click-gui module list. */
    public static void openClickGui() {
        openScreen(new ClickGuiScreen());
    }

    /** Closes the current screen (if any). */
    public static void closeScreen() {
        openScreen(null);
    }

    /** Whether Zephyr's click-gui is currently open. */
    public static boolean isClickGuiOpen() {
        return Minecraft.getInstance().gui.screen() instanceof ClickGuiScreen;
    }

    /** Advances to the next screen in the Tab cycle (built-ins, then addon screens). */
    public static void cycleNext() {
        Minecraft client = Minecraft.getInstance();
        Screen current = client.gui.screen();
        Screen next = ZephyrScreen.cycleNeighbor(current, 1);
        if (next != null) {
            client.gui.setScreen(next);
        }
    }

    /** Goes back one screen in the Tab cycle (wrapping through the built-ins). */
    public static void cyclePrevious() {
        Minecraft client = Minecraft.getInstance();
        Screen current = client.gui.screen();
        Screen previous = ZephyrScreen.cycleNeighbor(current, -1);
        if (previous != null) {
            client.gui.setScreen(previous);
        }
    }

    // ---------------------------------------------------------------- feedback & utilities

    /**
     * Shows a toast notification on the HUD (hidden while Stealth Mode is active).
     *
     * @param text  the message to display
     * @param color the accent color for the toast's top strip, e.g. {@code 0xFF7B4DFF}
     */
    public static void notify(String text, int color) {
        NotificationManager.notifyText(text, color);
    }

    /** Adds a client-side system chat message (never sent to the server). */
    public static void sendChatMessage(String text) {
        CommandManager.sendMessage(text);
    }

    /** Persists every module's state to {@code modules.json} and refreshes the active profile. */
    public static void saveConfig() {
        ModuleManager.saveAll();
    }

    /** Runs the action after the given number of client ticks. */
    public static void scheduleTicks(int delayTicks, Runnable action) {
        TickScheduler.schedule(delayTicks, action);
    }

    /** Runs the action after the given number of milliseconds (on the client thread). */
    public static void scheduleMillis(long delayMillis, Runnable action) {
        MillisScheduler.schedule(delayMillis, action);
    }

    /** The running Minecraft client instance. */
    public static Minecraft getClient() {
        return Minecraft.getInstance();
    }
}
