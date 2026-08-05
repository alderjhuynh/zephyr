package com.zephyr.client.configplusgui;

import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public final class Keybind {
    public static final int UNSET = GLFW.GLFW_KEY_UNKNOWN;
    public static final int MAX_KEYS = 3;
    public static final Keybind NONE = new Keybind(UNSET, UNSET, UNSET);

    private final int[] keys;
    private boolean conflicted;

    public Keybind(int a, int b, int c) {
        this.keys = new int[]{a, b, c};
    }

    public Keybind(int[] keys) {
        this.keys = new int[MAX_KEYS];
        Arrays.fill(this.keys, UNSET);
        for (int i = 0; i < Math.min(MAX_KEYS, keys.length); i++) {
            this.keys[i] = keys[i];
        }
    }

    public static Keybind of(List<Integer> pressed) {
        int[] keys = new int[MAX_KEYS];
        Arrays.fill(keys, UNSET);
        for (int i = 0; i < Math.min(MAX_KEYS, pressed.size()); i++) {
            keys[i] = pressed.get(i);
        }
        return new Keybind(keys);
    }

    public int[] keys() {
        return keys;
    }

    public boolean isSet() {
        return keys[0] != UNSET;
    }

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

    public void setConflicted(boolean conflicted) {
        this.conflicted = conflicted;
    }
}