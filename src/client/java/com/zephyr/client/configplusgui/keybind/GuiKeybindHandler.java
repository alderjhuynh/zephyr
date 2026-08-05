package com.zephyr.client.configplusgui.keybind;

import net.minecraft.client.Minecraft;

public final class GuiKeybindHandler {
    public void tick(Minecraft client) {
        KeybindManager.tick(client);
    }
}