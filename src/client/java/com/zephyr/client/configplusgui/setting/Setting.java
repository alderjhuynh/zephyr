package com.zephyr.client.configplusgui.setting;

/**
 * Base class for a typed, named configuration value owned by a {@link Module}. Concrete
 * subtypes ({@link BooleanSetting}, {@link NumberSetting}, {@link EnumSetting},
 * {@link StringSetting}, {@link ListSetting}) are exposed via
 * {@link Module#getSettings()} so the click-gui can render type-appropriate controls, and
 * are (de)serialized by {@link com.zephyr.client.configplusgui.config.ConfigManager}.
 */
public abstract class Setting<T> {
    private final String name;
    private T value;

    /** Creates a setting with a given name and initial (default) value. */
    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    /** The setting's display name, also used as its config key. */
    public final String getName() {
        return name;
    }

    /** The setting's current value. */
    public T get() {
        return value;
    }

    /** Writes the value without any subtype-specific validation or clamping. */
    protected void setValue(T value) {
        this.value = value;
    }
}