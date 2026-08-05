package com.zephyr.client.configplusgui.keybind;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class GlfwKeyNames {
    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
        NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
        NAMES.put(GLFW.GLFW_KEY_ESCAPE, "Escape");
        NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
        NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
        NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
        NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "L Shift");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "R Shift");
        NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "L Ctrl");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "R Ctrl");
        NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "L Alt");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "R Alt");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SUPER, "L Super");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SUPER, "R Super");
        NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
        NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
        NAMES.put(GLFW.GLFW_KEY_UP, "Up");
        NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
        NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock");
        NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "Page Up");
        NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down");
        NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
        NAMES.put(GLFW.GLFW_KEY_END, "End");
        for (int f = 1; f <= 12; f++) {
            NAMES.put(GLFW.GLFW_KEY_F1 + (f - 1), "F" + f);
        }
    }

    private GlfwKeyNames() {
    }

    public static String label(int key) {
        String known = NAMES.get(key);
        if (known != null) return known;

        String glfwName = GLFW.glfwGetKeyName(key, 0);
        if (glfwName != null && !glfwName.isBlank()) {
            return glfwName.toUpperCase();
        }
        return "Key " + key;
    }
}