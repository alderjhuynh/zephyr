package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Attempts a Lunge enchantment swap when attacking without a valid target in the crosshair:
 * the LungeSwap attack mixin swaps to the best Lunge-enchanted spear in the hotbar so the
 * swing registers as a lunging attack.
 */
public final class LungeSwap extends Module {
    public static final LungeSwap INSTANCE = new LungeSwap();
    private LungeSwap() {
        super("Lunge Swap", "Automatically attempts a Lunge Swap when attacking without a target", Category.COMBAT);
    }
}
