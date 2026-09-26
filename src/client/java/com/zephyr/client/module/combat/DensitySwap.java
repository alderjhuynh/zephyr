package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Enables the Density enchantment swap while falling a long distance: whenever the player
 * attacks with a fall distance at or above {@link #minFall}, the DensitySwap attack mixin
 * swaps to the best Density-enchanted mace in the hotbar before attacking.
 */
public final class DensitySwap extends Module {
    public static final DensitySwap INSTANCE = new DensitySwap();

    /** Minimum fall distance (in blocks) above which the Density swap is performed. */
    public NumberSetting minFall = new NumberSetting("Minimum Fall Distance", 7D, 0.00D, 20.0D, 0.5D);

    /** When enabled, delays swapping back to the original slot (vanilla-plausible). */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Ticks to wait before swapping back to the original slot in legit mode. */
    public final NumberSetting delay = new NumberSetting("Delay", 1.0, 0.0, 20.0, 1.0);

    /** Alias for compatibility with InstaCart/XBowCart/LungeSwap naming (Placement Delay). */
    public final NumberSetting placementDelay = delay;

    /** Max random extra ticks added to legit delay for humanlike jitter. */
    public final NumberSetting jitter = new NumberSetting("Jitter", 0.0, 0.0, 6.0, 1.0);

    private DensitySwap() {
        super("Density Swap", "Enables Density Swapping over a certain fall distance", Category.COMBAT);
        addSetting(minFall);
        addSetting(legit);
        addSetting(delay);
        addSetting(jitter);
    }
}
