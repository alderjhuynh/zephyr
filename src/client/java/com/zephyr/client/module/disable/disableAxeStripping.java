package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableAxeStripping extends Module {
    public static final disableAxeStripping INSTANCE = new disableAxeStripping();
    private disableAxeStripping() { super("Disable Axe Stripping", "Prevents axe stripping", Category.DISABLE); }
}
