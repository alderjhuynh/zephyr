package com.zephyr.client.configplusgui;

import net.minecraft.client.Minecraft;

public final class GuiKeybindHandler {
    public void tick(Minecraft client) {
        KeybindManager.tick(client);
    }
}