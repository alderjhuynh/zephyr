package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class InventoryPackets extends Module {
    public static final InventoryPackets INSTANCE = new InventoryPackets();
    private InventoryPackets() {
        super("Inventory Packets", "Skips sending packets when closing the inventory, allowing you to use the crafting slots as storage", Category.QOL);
    }
}
