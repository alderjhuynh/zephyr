package com.zephyr.client.configplusgui.keybind;

import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

/**
 * An immutable combo of up to {@link #MAX_KEYS} GLFW key codes that triggers a module
 * toggle or system action when all of them are pressed together. Unused slots hold
 * {@link #UNSET}. Combos are compared order-insensitively by {@link KeybindManager} for
 * conflict detection, which sets the mutable {@link #conflicted} flag. {@link #NONE} is the
 * canonical "no binding" value.
 */
public final class Keybind {
    /** GLFW code for "no key", also the value of empty slots in the combo. */
    public static final int UNSET = GLFW.GLFW_KEY_UNKNOWN;
    /** Maximum number of keys a single bind may combine. */
    public static final int MAX_KEYS = 3;
    /** The canonical empty bind (no keys set). */
    public static final Keybind NONE = new Keybind(UNSET, UNSET, UNSET);

    private final int[] keys;
    private boolean conflicted;

    /** Creates a bind from exactly three key codes (use {@link #UNSET} for unused slots). */
    public Keybind(int a, int b, int c) {
        this.keys = new int[]{a, b, c};
    }

    /** Creates a bind from an array; values beyond {@link #MAX_KEYS} are dropped, missing slots stay unset. */
    public Keybind(int[] keys) {
        this.keys = new int[MAX_KEYS];
        Arrays.fill(this.keys, UNSET);
        for (int i = 0; i < Math.min(MAX_KEYS, keys.length); i++) {
            this.keys[i] = keys[i];
        }
    }

    /** Builds a bind from the up-to-{@link #MAX_KEYS} keys that are currently held down. */
    public static Keybind of(List<Integer> pressed) {
        int[] keys = new int[MAX_KEYS];
        Arrays.fill(keys, UNSET);
        for (int i = 0; i < Math.min(MAX_KEYS, pressed.size()); i++) {
            keys[i] = pressed.get(i);
        }
        return new Keybind(keys);
    }

    /** The raw key codes, in slot order (unset slots hold {@link #UNSET}). */
    public int[] keys() {
        return keys;
    }

    /** Whether at least one key is bound (the first slot is not {@link #UNSET}). */
    public boolean isSet() {
        return keys[0] != UNSET;
    }

    /**
     * Renders the bind as a human-readable combo, e.g. {@code "L Ctrl + C"}, or
     * {@code "None"} when unset.
     */
    public String getLabel() {
        if (!isSet()) return "None";

        StringBuilder builder = new StringBuilder();
        for (int key : keys) {
            if (key == UNSET) continue;
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(GlfwKeyNames.label(key));
        }
        return builder.toString();
    }

    /** Whether another bind shares this combo, as computed by {@link KeybindManager}. */
    public boolean conflicted() {
        return conflicted;
    }

    /** Marks whether another bind shares this combo; set by {@link KeybindManager}. */
    public void setConflicted(boolean conflicted) {
        this.conflicted = conflicted;
    }
}