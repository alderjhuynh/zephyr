package com.zephyr.client.configplusgui;

import net.minecraft.client.Minecraft;

/**
 * Kept as the tick entry point your client setup already calls; all the actual
 * open/cycle/module-toggle keybind logic now lives in {@link KeybindManager}, which
 * additionally makes those combos configurable from the Keybinds screen instead of
 * hardcoding L + Enter here.
 */
public final class GuiKeybindHandler {
    public void tick(Minecraft client) {
        KeybindManager.tick(client);
    }
}