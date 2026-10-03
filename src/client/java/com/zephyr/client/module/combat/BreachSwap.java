package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class BreachSwap extends Module {
    public static final BreachSwap INSTANCE = new BreachSwap();
    public NumberSetting maxFall = new NumberSetting("Maximum Fall Distance", 2D, 0.00D, 10.0D, 0.1D);
    public final BooleanSetting legit = new BooleanSetting("Legit", false);
    public final NumberSetting delay = new NumberSetting("Delay", 1.0, 0.0, 20.0, 1.0);
    public final NumberSetting placementDelay = delay;
    public final NumberSetting jitter = new NumberSetting("Jitter", 0.0, 0.0, 6.0, 1.0);
    private BreachSwap() {
        super("Breach Swap", "Enables Breach Swapping under a certain fall distance", Category.COMBAT);
        addSetting(maxFall);
        addSetting(legit);
        addSetting(delay);
        addSetting(jitter);
    }
}
