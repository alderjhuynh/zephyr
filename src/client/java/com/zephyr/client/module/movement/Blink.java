package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class Blink extends Module {
    public static final Blink INSTANCE = new Blink();

    private Blink() {
        super("Blink", "Suppresses your position packets while enabled", Category.MOVEMENT);
    }
}
