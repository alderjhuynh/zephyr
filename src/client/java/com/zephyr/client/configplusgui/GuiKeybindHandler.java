package com.zephyr.client.configplusgui;

import com.zephyr.client.configplusgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric doesn't support multi-key keybindings out of the box, so the L+Enter combo
 * is detected manually by polling GLFW key state once per client tick.
 */
public final class GuiKeybindHandler {
    private boolean comboWasDown = false;

    public void tick(Minecraft client) {
        boolean comboDown = isKeyDown(client, GLFW.GLFW_KEY_L) && isKeyDown(client, GLFW.GLFW_KEY_ENTER);

        if (comboDown && !comboWasDown) {
            if (client.gui.screen() == null) {
                client.gui.setScreen(new ClickGuiScreen());
            } else if (client.gui.screen() instanceof ClickGuiScreen clickGui) {
                clickGui.onClose();
            }
        }

        comboWasDown = comboDown;
    }

    private static boolean isKeyDown(Minecraft client, int glfwKey) {
        long windowHandle = client.getWindow().handle();
        return GLFW.glfwGetKey(windowHandle, glfwKey) == GLFW.GLFW_PRESS;
    }
}
