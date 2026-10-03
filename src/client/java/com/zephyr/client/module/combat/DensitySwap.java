package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class DensitySwap extends Module {
    public static final DensitySwap INSTANCE = new DensitySwap();
    public NumberSetting minFall = new NumberSetting("Minimum Fall Distance", 7D, 0.00D, 20.0D, 0.5D);
    public final BooleanSetting legit = new BooleanSetting("Legit", false);
    public final NumberSetting delay = new NumberSetting("Delay", 1.0, 0.0, 20.0, 1.0);
    public final NumberSetting placementDelay = delay;
    public final NumberSetting jitter = new NumberSetting("Jitter", 0.0, 0.0, 6.0, 1.0);
    private DensitySwap() {
        super("Density Swap", "Enables Density Swapping over a certain fall distance", Category.COMBAT);
        addSetting(minFall);
        addSetting(legit);
        addSetting(delay);
        addSetting(jitter);
    }
}
