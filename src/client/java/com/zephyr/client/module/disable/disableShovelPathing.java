package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that prevents shovels from turning grass and dirt into
 * path blocks. A simple toggle with no settings; it is backed by
 * {@code ShovelItemMixin}, which cancels the shovel's {@code useOn} interaction while
 * enabled.
 */
public final class disableShovelPathing extends Module {
    public static final disableShovelPathing INSTANCE = new disableShovelPathing();
    private disableShovelPathing() { super("Disable Shovel Pathing", "Prevents shovel pathing", Category.DISABLE); }
}
