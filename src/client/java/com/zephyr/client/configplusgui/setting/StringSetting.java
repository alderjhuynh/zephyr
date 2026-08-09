package com.zephyr.client.configplusgui.setting;

public final class StringSetting extends Setting<String> {

    public StringSetting(String name, String defaultValue) {
        super(name, defaultValue == null ? "" : defaultValue);
    }

    public void set(String value) {
        setValue(value == null ? "" : value);
    }
}
