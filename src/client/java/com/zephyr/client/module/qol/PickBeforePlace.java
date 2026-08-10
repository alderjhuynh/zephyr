package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Automatically performs a pick-block action before placing a block. The pick
 * is injected by the associated mixin just before a place action, ensuring the
 * correct block type is selected. The module itself carries no per-tick logic.
 */
public final class PickBeforePlace extends Module {
    public static final PickBeforePlace INSTANCE = new PickBeforePlace();
    private PickBeforePlace() {
        super("Pick Before Place", "Forces a block pick action before placing a block", Category.QOL);
    }
}
