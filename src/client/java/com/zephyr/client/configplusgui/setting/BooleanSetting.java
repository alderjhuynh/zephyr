package com.zephyr.client.configplusgui.setting;

/**
 * A {@link Setting} holding an on/off boolean, rendered as a checkbox row in the
 * click-gui. Used for module toggles within a module and for global flags such as
 * {@code Hotkey Popups}.
 */
public final class BooleanSetting extends Setting<Boolean> {

    /** Creates a boolean setting with the given default state. */
    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    /** Sets the value. */
    public void set(boolean value) {
        setValue(value);
    }

    /** Flips the current value. */
    public void toggle() {
        setValue(!get());
    }
}
