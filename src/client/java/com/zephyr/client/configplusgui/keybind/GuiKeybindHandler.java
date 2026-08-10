package com.zephyr.client.configplusgui.keybind;

import net.minecraft.client.Minecraft;

/**
 * Thin per-tick bridge used by the mod's client tick listener: forwards every tick to
 * {@link KeybindManager#tick(Minecraft)} so module and system keybinds are evaluated once
 * per frame. Kept separate so the listener only needs to know about this class.
 */
public final class GuiKeybindHandler {
    /** Polls all keybind state for the current frame. */
    public void tick(Minecraft client) {
        KeybindManager.tick(client);
    }
}