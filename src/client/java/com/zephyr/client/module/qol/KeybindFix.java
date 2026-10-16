package com.zephyr.client.module.qol;

import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Fixes the classic Linux keybinding bug where unbound keys are bound to
 * {@code key.keyboard.unknown} (SDL scancode 0, a value real key events can
 * produce on Linux) instead of a genuine "no key" sentinel. While enabled,
 * every unbind (controls-screen ESC, invalid options.txt entries, reset to an
 * unbound default) is redirected to {@code key.keyboard.none} (value -1),
 * which no physical key ever produces, so unbound mappings can never
 * spuriously trigger via {@code KeyMapping.click/set}.
 */
public final class KeybindFix extends Module {
    public static final KeybindFix INSTANCE = new KeybindFix();

    /** Translation key of the genuine "no key" sentinel. */
    public static final String NONE_NAME = "key.keyboard.none";
    /** Key value of the sentinel; no physical key ever produces -1. */
    public static final int NONE_VALUE = -1;

    private KeybindFix() {
        super("Keybind Fix", "Binds unbound keys to key.keyboard.none instead of key.keyboard.unknown", Category.QOL, true);
    }

    /**
     * The canonical "no key" binding, registered on the keyboard type by the
     * associated input mixin.
     */
    public static InputConstants.Key none() {
        return InputConstants.Type.KEYBOARD.getOrCreate(NONE_VALUE);
    }

    /** Whether the given key is the "no key" sentinel (keyboard type, value -1). */
    public static boolean isNone(InputConstants.Key key) {
        return key != null
                && key.getType() == InputConstants.Type.KEYBOARD
                && key.getValue() == NONE_VALUE;
    }
}
