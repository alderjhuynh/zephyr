package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableBlockBreakingCooldown extends Module {
    public static final disableBlockBreakingCooldown INSTANCE = new disableBlockBreakingCooldown();
    private disableBlockBreakingCooldown() { super("Disable Block Cooldown", "Removes block breaking cooldown", Category.DISABLE); }
}
