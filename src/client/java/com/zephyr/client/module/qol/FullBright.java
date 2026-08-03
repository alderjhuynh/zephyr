package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class FullBright extends Module {
    public static final FullBright INSTANCE = new FullBright();
    private FullBright() {
        super("FullBright", "Increases gamma", Category.QOL);
    }
}
