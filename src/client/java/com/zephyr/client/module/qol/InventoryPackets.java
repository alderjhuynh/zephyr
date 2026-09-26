package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Prevents the server from being notified when the inventory screen is closed,
 * letting items left in the player's crafting grid persist on the client. The
 * packet suppression is applied by the associated mixin while this module is
 * enabled.
 */
public final class InventoryPackets extends Module {
    public static final InventoryPackets INSTANCE = new InventoryPackets();
    private InventoryPackets() {
        super("Inventory Packets", "Skips sending packets when closing the inventory, allowing you to use the crafting slots as storage", Category.QOL);
    }
}
