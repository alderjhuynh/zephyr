package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class PickBeforePlace extends Module {
    public static final PickBeforePlace INSTANCE = new PickBeforePlace();
    private PickBeforePlace() {
        super("Pick Before Place", "Forces a block pick action before placing a block", Category.QOL);
    }
}
