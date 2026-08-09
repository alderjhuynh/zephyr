package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class Knockback extends Module {
    public static final Knockback INSTANCE = new Knockback();

    public final NumberSetting amount = new NumberSetting("Amount", 0.5D, 0D, 1D, 0.05D);

    private Knockback() {
        super("Knockback", "Reduces the amount of knockback you take", Category.COMBAT);
        addSetting(amount);
    }
}
