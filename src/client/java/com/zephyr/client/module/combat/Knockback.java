package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Reduces the knockback the local player takes by scaling down incoming velocity changes.
 * Consumed by the Knockback client packet listener mixin, which multiplies set-motion packets
 * by {@link #amount}.
 */
public final class Knockback extends Module {
    public static final Knockback INSTANCE = new Knockback();

    /** Multiplier applied to incoming knockback velocity (0.0 = none, 1.0 = full). */
    public final NumberSetting amount = new NumberSetting("Amount", 0.5D, 0D, 1D, 0.05D);

    private Knockback() {
        super("Knockback", "Reduces the amount of knockback you take", Category.COMBAT);
        addSetting(amount);
    }
}
