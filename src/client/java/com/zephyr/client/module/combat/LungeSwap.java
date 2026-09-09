package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Attempts a Lunge enchantment swap when attacking without a valid target in the crosshair:
 * the LungeSwap attack mixin swaps to the best Lunge-enchanted spear in the hotbar so the
 * swing registers as a lunging attack.
 */
public final class LungeSwap extends Module {
    public static final LungeSwap INSTANCE = new LungeSwap();

    /** When enabled, delays swapping back to the original slot (vanilla-plausible). */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Ticks to wait before swapping back to the original slot in legit mode. */
    public final NumberSetting delay = new NumberSetting("Delay", 1.0, 0.0, 20.0, 1.0);

    /** Alias for compatibility with InstaCart/XBowCart naming (Placement Delay). */
    public final NumberSetting placementDelay = delay;

    private LungeSwap() {
        super("Lunge Swap", "Automatically attempts a Lunge Swap when attacking without a target", Category.COMBAT);
        addSetting(legit);
        addSetting(delay);
    }
}
