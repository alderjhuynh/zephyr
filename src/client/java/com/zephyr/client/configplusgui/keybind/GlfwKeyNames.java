package com.zephyr.client.configplusgui.keybind;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps key constants (SDL scancodes, see {@link InputConstants}) to short
 * human-readable labels used when rendering keybinds in the
 * {@link com.zephyr.client.configplusgui.screen.KeybindGuiScreen} and elsewhere.
 * Commonly used keys (modifiers, navigation, function keys) have curated names;
 * everything else falls back to the platform key name or a "Key N" placeholder.
 * Used by {@link Keybind#getLabel()}.
 */
public final class GlfwKeyNames {
    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(InputConstants.KEY_SPACE, "Space");
        NAMES.put(InputConstants.KEY_RETURN, "Enter");
        NAMES.put(InputConstants.KEY_ESCAPE, "Escape");
        NAMES.put(InputConstants.KEY_TAB, "Tab");
        NAMES.put(InputConstants.KEY_BACKSPACE, "Backspace");
        NAMES.put(InputConstants.KEY_DELETE, "Delete");
        NAMES.put(InputConstants.KEY_INSERT, "Insert");
        NAMES.put(InputConstants.KEY_LSHIFT, "L Shift");
        NAMES.put(InputConstants.KEY_RSHIFT, "R Shift");
        NAMES.put(InputConstants.KEY_LCONTROL, "L Ctrl");
        NAMES.put(InputConstants.KEY_RCONTROL, "R Ctrl");
        NAMES.put(InputConstants.KEY_LALT, "L Alt");
        NAMES.put(InputConstants.KEY_RALT, "R Alt");
        NAMES.put(InputConstants.KEY_LGUI, "L Super");
        NAMES.put(InputConstants.KEY_RGUI, "R Super");
        NAMES.put(InputConstants.KEY_LEFT, "Left");
        NAMES.put(InputConstants.KEY_RIGHT, "Right");
        NAMES.put(InputConstants.KEY_UP, "Up");
        NAMES.put(InputConstants.KEY_DOWN, "Down");
        NAMES.put(InputConstants.KEY_CAPSLOCK, "Caps Lock");
        NAMES.put(InputConstants.KEY_PAGEUP, "Page Up");
        NAMES.put(InputConstants.KEY_PAGEDOWN, "Page Down");
        NAMES.put(InputConstants.KEY_HOME, "Home");
        NAMES.put(InputConstants.KEY_END, "End");
        for (int f = 1; f <= 12; f++) {
            NAMES.put(InputConstants.KEY_F1 + (f - 1), "F" + f);
        }
    }

    private GlfwKeyNames() {
    }

    /**
     * Returns the display name for a key code.
     *
     * @param key a key constant, e.g. {@code InputConstants.KEY_SPACE}
     * @return the curated name if known, otherwise the platform key name, otherwise "Key N"
     */
    public static String label(int key) {
        String known = NAMES.get(key);
        if (known != null) return known;

        try {
            String platformName = InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
            if (platformName != null && !platformName.isBlank()) {
                return platformName.toUpperCase();
            }
        } catch (RuntimeException ignored) {
        }
        return "Key " + key;
    }
}
