package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Attempts to perform a pearl catch when throwing an Ender Pearl, waiting
 * {@link #delay} ticks if {@link #legit} is active.
 */
public final class PearlCatch extends Module {
    public final BooleanSetting legit = new BooleanSetting("Legit", false);
    public final NumberSetting delay = new NumberSetting("Delay", 1.0, 0.0, 20.0, 1.0);

    public static final PearlCatch INSTANCE = new PearlCatch();
    private PearlCatch() {
        super("Pearl Catch","Catches a thrown Ender Pearl with a Wind Charge.", Category.COMBAT);
        addSetting(legit);
        addSetting(delay);
    }
}
