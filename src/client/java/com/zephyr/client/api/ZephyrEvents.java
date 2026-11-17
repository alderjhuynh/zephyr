package com.zephyr.client.api;

import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight event bus for addons. Register listeners at any time (typically from
 * your {@link ZephyrAddon#onInitializeZephyr()}); every listener receives events for
 * as long as the client runs. All dispatch happens on the main client thread, so
 * listeners can safely touch the world, the player and Zephyr's registries.
 */
public final class ZephyrEvents {
    private static final List<Consumer<ModuleToggleEvent>> MODULE_TOGGLED = new CopyOnWriteArrayList<>();
    private static final List<Consumer<SettingChangedEvent>> SETTING_CHANGED = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Minecraft>> CLIENT_TICK = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Minecraft>> WORLD_JOIN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Minecraft>> WORLD_DISCONNECT = new CopyOnWriteArrayList<>();

    private ZephyrEvents() {
    }

    /** Fired whenever any module (built-in or addon) transitions between enabled and disabled. */
    public record ModuleToggleEvent(Module module, boolean enabled) {
    }

    /** Fired whenever any setting's value is written. The setting doesn't know its owner module. */
    public record SettingChangedEvent(Setting<?> setting) {
    }

    /** Registers a listener for {@link ModuleToggleEvent}s. */
    public static void onModuleToggled(Consumer<ModuleToggleEvent> listener) {
        MODULE_TOGGLED.add(listener);
    }

    /** Registers a listener for {@link SettingChangedEvent}s. */
    public static void onSettingChanged(Consumer<SettingChangedEvent> listener) {
        SETTING_CHANGED.add(listener);
    }

    /** Registers a listener fired at the end of every client tick, screen open or not. */
    public static void onClientTick(Consumer<Minecraft> listener) {
        CLIENT_TICK.add(listener);
    }

    /** Registers a listener fired when the client joins a world/server. */
    public static void onWorldJoin(Consumer<Minecraft> listener) {
        WORLD_JOIN.add(listener);
    }

    /** Registers a listener fired when the client leaves a world/server. */
    public static void onWorldDisconnect(Consumer<Minecraft> listener) {
        WORLD_DISCONNECT.add(listener);
    }

    /** Dispatches a module toggle; called by {@link Module#setEnabled}. */
    public static void fireModuleToggled(Module module, boolean enabled) {
        for (Consumer<ModuleToggleEvent> listener : MODULE_TOGGLED) {
            listener.accept(new ModuleToggleEvent(module, enabled));
        }
    }

    /** Dispatches a setting change; called by {@link Setting#setValue}. */
    public static void fireSettingChanged(Setting<?> setting) {
        for (Consumer<SettingChangedEvent> listener : SETTING_CHANGED) {
            listener.accept(new SettingChangedEvent(setting));
        }
    }

    /** Dispatches a client tick; called by Zephyr's client initializer. */
    public static void fireClientTick(Minecraft client) {
        for (Consumer<Minecraft> listener : CLIENT_TICK) {
            listener.accept(client);
        }
    }

    /** Dispatches a world join; called by Zephyr's client initializer. */
    public static void fireWorldJoin(Minecraft client) {
        for (Consumer<Minecraft> listener : WORLD_JOIN) {
            listener.accept(client);
        }
    }

    /** Dispatches a world disconnect; called by Zephyr's client initializer. */
    public static void fireWorldDisconnect(Minecraft client) {
        for (Consumer<Minecraft> listener : WORLD_DISCONNECT) {
            listener.accept(client);
        }
    }
}
