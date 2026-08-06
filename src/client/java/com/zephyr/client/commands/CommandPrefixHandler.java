package com.zephyr.client.commands;

import com.zephyr.client.configplusgui.keybind.GlfwKeyNames;
import com.zephyr.client.configplusgui.keybind.Keybind;
import com.zephyr.client.configplusgui.keybind.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

public final class CommandPrefixHandler {
    private static boolean wasDown;

    private CommandPrefixHandler() {
    }

    public static void tick(Minecraft client) {
        Keybind bind = KeybindManager.get(KeybindManager.SystemAction.COMMAND_PREFIX);
        if (!bind.isSet()) {
            wasDown = false;
            return;
        }

        boolean down = isDown(client, bind);
        if (down && !wasDown && client.gui.screen() == null) {
            client.gui.setScreen(new ChatScreen(prefixValue(bind), false));
        }
        wasDown = down;
    }

    /** The literal text the Command Prefix keybind currently resolves to, or null if unset. */
    public static String currentPrefix() {
        Keybind bind = KeybindManager.get(KeybindManager.SystemAction.COMMAND_PREFIX);
        return bind.isSet() ? prefixValue(bind) : null;
    }

    private static boolean isDown(Minecraft client, Keybind bind) {
        long windowHandle = client.getWindow().handle();
        for (int key : bind.keys()) {
            if (key == Keybind.UNSET) continue;
            if (GLFW.glfwGetKey(windowHandle, key) != GLFW.GLFW_PRESS) return false;
        }
        return true;
    }

    private static String prefixValue(Keybind bind) {
        StringBuilder builder = new StringBuilder();
        for (int key : bind.keys()) {
            if (key == Keybind.UNSET) continue;
            String name = GLFW.glfwGetKeyName(key, 0);
            if (name == null || name.isBlank()) {
                name = GlfwKeyNames.label(key);
            }
            builder.append(name);
        }
        return builder.toString();
    }
}
