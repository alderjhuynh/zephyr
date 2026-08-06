package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableBossbar extends Module {
    public static final disableBossbar INSTANCE = new disableBossbar();
    private disableBossbar() { super("Disable Bossbar", "Hides bossbars", Category.DISABLE); }
}
