package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class AutoWalk extends Module {
    public static final AutoWalk INSTANCE = new AutoWalk();

    private AutoWalk() {
        super("Auto Walk", "Walks forward automatically without holding W", Category.MOVEMENT);
    }
}
