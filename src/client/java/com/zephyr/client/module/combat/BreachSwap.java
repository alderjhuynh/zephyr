package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Enables the Breach enchantment swap while falling a short distance: whenever the player
 * attacks with a fall distance at or below {@link #maxFall}, the BreachSwap attack mixin
 * swaps to the best Breach-enchanted mace in the hotbar before attacking.
 */
public final class BreachSwap extends Module {
    public static final BreachSwap INSTANCE = new BreachSwap();

    /** Maximum fall distance (in blocks) under which the Breach swap is performed. */
    public NumberSetting maxFall = new NumberSetting("Maximum Fall Distance", 2D, 0.00D, 10.0D, 0.1D);
    private BreachSwap() {
        super("Breach Swap", "Enables Breach Swapping under a certain fall distance", Category.COMBAT);
        addSetting(maxFall);
    }
}
