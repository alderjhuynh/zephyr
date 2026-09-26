package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Movement module that keeps the player walking forward without holding the forward key. The
 * AutoWalk KeyboardInput mixin forces the forward flag in the player's input each tick while
 * the module is enabled, leaving the other movement inputs untouched.
 */
public final class AutoWalk extends Module {
    public static final AutoWalk INSTANCE = new AutoWalk();

    private AutoWalk() {
        super("Auto Walk", "Walks forward automatically without holding W", Category.MOVEMENT);
    }
}
