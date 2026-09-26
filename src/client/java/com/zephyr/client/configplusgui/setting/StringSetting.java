package com.zephyr.client.configplusgui.setting;

/**
 * A {@link Setting} holding free-form text, rendered as an editable text field in the
 * click-gui. A {@code null} default or assignment is normalized to the empty string.
 */
public final class StringSetting extends Setting<String> {

    /** Creates a string setting; {@code null} defaults become empty strings. */
    public StringSetting(String name, String defaultValue) {
        super(name, defaultValue == null ? "" : defaultValue);
    }

    /** Sets the value; {@code null} is normalized to an empty string. */
    public void set(String value) {
        setValue(value == null ? "" : value);
    }
}
