package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;

public final class DensitySwap extends Module {
    public static final DensitySwap INSTANCE = new DensitySwap();
    public NumberSetting minFall = new NumberSetting("Minimum Fall Distance", 7D, 0.00D, 20.0D, 0.5D);
    private DensitySwap() {
        super("Density Swap", "Enables Density Swapping over a certain fall distance", Category.COMBAT);
        addSetting(minFall);
    }
}
