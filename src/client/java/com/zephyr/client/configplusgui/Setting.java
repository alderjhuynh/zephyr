package com.zephyr.client.configplusgui;

/**
 * A single configurable value exposed by a {@link com.zephyr.client.module}
 * in the click-gui's right-click customization panel. Extend this for new setting
 * types (see {@link BooleanSetting}, {@link NumberSetting}, {@link EnumSetting}); the
 * gui and {@code ConfigManager} both dispatch on the concrete subclass.
 */
public abstract class Setting<T> {
    private final String name;
    private T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public final String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    protected void setValue(T value) {
        this.value = value;
    }
}