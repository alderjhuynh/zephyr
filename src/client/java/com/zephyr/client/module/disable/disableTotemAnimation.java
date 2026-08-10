package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableTotemAnimation extends Module {
    public static final disableTotemAnimation INSTANCE = new disableTotemAnimation();
    private disableTotemAnimation() { super("Disable Totem Animation", "Prevents the totem of undying pop-up animation and effects", Category.DISABLE); }
}
