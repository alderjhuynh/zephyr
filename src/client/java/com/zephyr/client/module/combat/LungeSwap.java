package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class LungeSwap extends Module {
    public static final LungeSwap INSTANCE = new LungeSwap();
    private LungeSwap() {
        super("Lunge Swap", "Automatically attempts a Lunge Swap when attacking without a target", Category.COMBAT);
    }
}
