package com.zephyr.client.configplusgui.setting;

public final class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void set(boolean value) {
        setValue(value);
    }

    public void toggle() {
        setValue(!get());
    }
}
