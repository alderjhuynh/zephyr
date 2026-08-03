package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableShovelPathing extends Module {
    public static final disableShovelPathing INSTANCE = new disableShovelPathing();
    private disableShovelPathing() { super("Disable Shovel Pathing", "Prevents shovel pathing", Category.DISABLE); }
}
