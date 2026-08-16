package com.zephyr.client.commands;

import com.zephyr.client.configplusgui.keybind.GlfwKeyNames;
import com.zephyr.client.configplusgui.keybind.Keybind;
import com.zephyr.client.configplusgui.keybind.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Detects the configured Command Prefix keybind and opens the chat screen
 * pre-filled with the prefix so the player can type a Zephyr command. The prefix
 * itself is derived from the key names currently bound to the keybind (e.g. a
 * bound '.' key yields a "." prefix). Also exposes the resolved prefix so other
 * components ({@link CommandManager}) can recognize command messages.
 */
public final class CommandPrefixHandler {
    private static boolean wasDown;

    private CommandPrefixHandler() {
    }

    /**
     * Polls the Command Prefix keybind each tick. On the rising edge of the
     * keybind press (while no screen is open), opens the chat screen pre-filled
     * with the prefix text. Polling is skipped entirely while any screen is open.
     *
     * @param client the Minecraft client instance
     */
    public static void tick(Minecraft client) {
        if (client.gui.screen() != null) {
            return;
        }

        Keybind bind = KeybindManager.get(KeybindManager.SystemAction.COMMAND_PREFIX);
        if (!bind.isSet()) {
            wasDown = false;
            return;
        }

        boolean down = isDown(client, bind);
        if (down && !wasDown) {
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
