package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableInventoryEffectRendering extends Module {
    public static final disableInventoryEffectRendering INSTANCE = new disableInventoryEffectRendering();
    private disableInventoryEffectRendering() { super("Disable Inventory Effects", "Hides inventory status effects", Category.DISABLE); }
}
