package com.zephyr.client.configplusgui;

import com.zephyr.client.configplusgui.Setting;
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

    protected Module(String name, String description, Category category) {
        this(name, description, category, false);
    }

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

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final Category getCategory() {
        return category;
    }

    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public final boolean isEnabled() {
        return enabled;
    }

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