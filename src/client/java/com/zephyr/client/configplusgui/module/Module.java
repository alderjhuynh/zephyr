package com.zephyr.client.configplusgui.module;

import com.zephyr.client.configplusgui.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every toggleable feature. Extend this, register a singleton instance
 * via ModuleManager#register(Module), and it will automatically show up in the
 * click-gui, get persisted to disk, and receive tick calls while enabled.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;

    /** Creates a module that starts disabled. */
    protected Module(String name, String description, Category category) {
        this(name, description, category, false);
    }

    /** Creates a module with an explicit initial enabled state (e.g. for always-on features). */
    protected Module(String name, String description, Category category, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    /**
     * Call from a subclass constructor to expose a setting in the click-gui's
     * right-click customization panel.
     */
    protected final void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    /** The module's unique display name, also used as the config/profile key. */
    public final String getName() {
        return name;
    }

    /** Short description shown in the click-gui. */
    public final String getDescription() {
        return description;
    }

    /** The tab this module appears under in the click-gui. */
    public final Category getCategory() {
        return category;
    }

    /** The module's exposed settings, read-only; populated via {@link #addSetting}. */
    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /** Whether the module is currently enabled. */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the module, firing {@link #onEnable()} / {@link #onDisable()}
     * on the transition. No-ops when the state is unchanged.
     */
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /**
     * Sets the enabled flag without firing {@link #onEnable()}/{@link #onDisable()}.
     * Used by the config loader on startup, before the game is necessarily ready
     * for a module's enable-side-effects to run.
     */
    public final void setEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    /** Flips the module between enabled and disabled. */
    public final void toggle() {
        setEnabled(!enabled);
    }

    /** Called once when the module transitions from disabled to enabled. */
    protected void onEnable() {
    }

    /** Called once when the module transitions from enabled to disabled. */
    protected void onDisable() {
    }

    /** Called every client tick while this module is enabled. */
    public void tick(Minecraft client) {
    }
}