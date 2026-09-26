package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that prevents axes from stripping logs and wood blocks
 * when used on them. A simple toggle with no settings; it is backed by
 * {@code AxeItemMixin}, which cancels the axe's {@code useOn} interaction while enabled.
 */
public final class disableAxeStripping extends Module {
    public static final disableAxeStripping INSTANCE = new disableAxeStripping();
    private disableAxeStripping() { super("Disable Axe Stripping", "Prevents axe stripping", Category.DISABLE); }
}
