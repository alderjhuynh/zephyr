package com.zephyr.client.configplusgui.setting;

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