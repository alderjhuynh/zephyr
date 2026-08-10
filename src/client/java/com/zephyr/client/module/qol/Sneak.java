package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Automatically holds the sneak state, so the player sneaks even without
 * pressing the sneak key. The sneak input is forced by the associated mixin
 * while this module is enabled.
 */
public final class Sneak extends Module {
    public static final Sneak INSTANCE = new Sneak();
    private Sneak() {
        super("Sneak", "Automatically sneaks", Category.QOL);
    }
}
