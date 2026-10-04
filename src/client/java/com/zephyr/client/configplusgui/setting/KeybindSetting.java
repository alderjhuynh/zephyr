package com.zephyr.client.configplusgui.setting;

import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.configplusgui.keybind.Keybind;
import com.zephyr.client.configplusgui.keybind.KeybindManager;
import net.minecraft.client.Minecraft;

/**
 * A {@link Setting} holding a rebindable key combo of up to {@link Keybind#MAX_KEYS}
 * keys — the same combo format as the keybinds screen — that a module polls to drive
 * its own actions. A module may own any number of these (one per action):
 *
 * <pre>
 * private final KeybindSetting openMap = new KeybindSetting("Open Map", InputConstants.KEY_M);
 *
 * // in the constructor:
 * addSetting(openMap);
 *
 * // in tick():
 * if (openMap.consumeClick()) { ... }
 * </pre>
 *
 * <p>Polling semantics:
 * <ul>
 *   <li>{@link #isDown()} is level-triggered: true for every tick the combo is held.</li>
 *   <li>{@link #consumeClick()} is edge-triggered: true once per press, for wiring
 *   one-shot events (open a screen, trigger an ability). Call it every tick while the
 *   module is enabled; missed ticks can swallow an edge.</li>
 * </ul>
 *
 * <p>Both poll methods return false while input is suppressed (typing in a text field
 * or capturing a bind in either GUI), and they keep the edge state in sync while
 * suppressed so a press that started mid-suppression never fires later. Edges of
 * disabled modules are re-synced every tick by {@link KeybindManager}, so enabling
 * a module while its keys are held does not fire either — no manual
 * {@link #resetEdge()} needed in the common case.
 */
public final class KeybindSetting extends Setting<Keybind> {
    private boolean wasDown;

    /** Creates an unbound keybind setting. */
    public KeybindSetting(String name) {
        super(name, Keybind.NONE);
    }

    /** Creates a keybind setting with the given default combo ({@code null} becomes unbound). */
    public KeybindSetting(String name, Keybind defaultValue) {
        super(name, defaultValue == null ? Keybind.NONE : defaultValue);
    }

    /** Creates a keybind setting bound to the given keys (extra keys beyond {@link Keybind#MAX_KEYS} are dropped). */
    public KeybindSetting(String name, int... defaultKeys) {
        super(name, defaultKeys == null ? Keybind.NONE : new Keybind(defaultKeys));
    }

    /** Rebinds to the given combo ({@code null} clears it) and re-syncs the press edge. */
    public void set(Keybind bind) {
        setValue(bind == null ? Keybind.NONE : bind);
        wasDown = isPhysicallyDown();
    }

    /** Rebinds to the given keys (extra keys beyond {@link Keybind#MAX_KEYS} are dropped). */
    public void set(int... keys) {
        set(keys == null ? Keybind.NONE : new Keybind(keys));
    }

    /** Clears the bind back to unbound. */
    public void clear() {
        set(Keybind.NONE);
    }

    /**
     * Whether the whole combo is currently held. False when unbound or while input
     * is suppressed (typing / capturing).
     */
    public boolean isDown() {
        if (isSuppressed()) return false;
        return isPhysicallyDown();
    }

    /**
     * Whether the combo was freshly pressed since the last poll. True at most once per
     * physical press; always call this every tick while enabled. False when unbound or
     * while input is suppressed (the edge is re-synced, not queued).
     */
    public boolean consumeClick() {
        boolean down = isPhysicallyDown();
        if (isSuppressed()) {
            wasDown = down;
            return false;
        }
        boolean clicked = down && !wasDown;
        wasDown = down;
        return clicked;
    }

    /**
     * Re-syncs the press-edge state to the currently held keys without firing.
     * Called automatically every tick for disabled modules; call it manually when a
     * module starts ignoring its binds temporarily (e.g. while one of its own screens
     * is open) to avoid a stale edge firing afterwards.
     */
    public void resetEdge() {
        wasDown = isPhysicallyDown();
    }

    /** Raw physical state, ignoring suppression. */
    private boolean isPhysicallyDown() {
        Keybind bind = get();
        if (bind == null || !bind.isSet()) return false;
        for (int key : bind.keys()) {
            if (key == Keybind.UNSET) continue;
            if (!InputConstants.isKeyDown(key)) return false;
        }
        return true;
    }

    private static boolean isSuppressed() {
        return KeybindManager.isInputSuppressed(Minecraft.getInstance());
    }
}
