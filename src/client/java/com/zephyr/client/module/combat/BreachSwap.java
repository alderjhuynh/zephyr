package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class BreachSwap extends Module {
    public static final BreachSwap INSTANCE = new BreachSwap();
    public NumberSetting maxFall = new NumberSetting("Maximum Fall Distance", 2D, 0.00D, 10.0D, 0.1D);
    private BreachSwap() {
        super("Breach Swap", "Enables Breach Swapping under a certain fall distance", Category.COMBAT);
        addSetting(maxFall);
    }
}
