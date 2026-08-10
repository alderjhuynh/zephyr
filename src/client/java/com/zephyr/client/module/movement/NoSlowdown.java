package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class NoSlowdown extends Module {
    public static final NoSlowdown INSTANCE = new NoSlowdown();

    private NoSlowdown() {
        super("No Slowdown", "Cancels the movement speed reduction from using items, walking in webs, or pushing through water",
                Category.MOVEMENT);
    }
}
