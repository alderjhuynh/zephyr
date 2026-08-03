package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class Sneak extends Module {
    public static final Sneak INSTANCE = new Sneak();
    private Sneak() {
        super("Sneak", "Automatically sneaks", Category.QOL);
    }
}
