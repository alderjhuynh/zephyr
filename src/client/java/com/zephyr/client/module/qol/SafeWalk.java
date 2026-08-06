package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class SafeWalk extends Module {
    public static final SafeWalk INSTANCE = new SafeWalk();
    private SafeWalk() {
        super("Safe Walk", "Prevents you from walking off block edges until you jump", Category.QOL);
    }
}
