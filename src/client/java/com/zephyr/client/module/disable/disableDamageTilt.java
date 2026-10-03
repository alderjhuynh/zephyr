package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableDamageTilt extends Module {
    public static final disableDamageTilt INSTANCE = new disableDamageTilt();

    private disableDamageTilt() {
        super("Disable Damage Tilt", "Removes the camera tilt when you take damage", Category.DISABLE);
    }
}
