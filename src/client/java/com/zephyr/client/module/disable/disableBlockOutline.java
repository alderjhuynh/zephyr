package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableBlockOutline extends Module {
    public static final disableBlockOutline INSTANCE = new disableBlockOutline();
    private disableBlockOutline() { super("Disable Block Outline", "Hides the black outline on the targeted block", Category.DISABLE); }
}
