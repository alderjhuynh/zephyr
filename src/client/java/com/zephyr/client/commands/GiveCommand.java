package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Port of {@code cgive}. Gives items in creative mode via client-side inventory manipulation.
 */
public final class GiveCommand extends Command {
    public static final GiveCommand INSTANCE = new GiveCommand();

    private GiveCommand() {
        super("give", "Give items in creative: .z give <item> [count] (alias: cgive)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).toList();
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (!mc.player.isCreative()) { CommandManager.sendMessage("You must be in creative mode"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z give <item> [count]"); return; }
        String itemStr = args[0];
        int count = 1;
        if (args.length >= 2) {
            try { count = Integer.parseInt(args[1]); } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid count"); return; }
            if (count < 1) count = 1; if (count > 64) count = 64;
        }
        Identifier id = itemStr.contains(":") ? Identifier.tryParse(itemStr) : Identifier.tryParse("minecraft:" + itemStr);
        if (id == null) { CommandManager.sendMessage("Invalid item: " + itemStr); return; }
        var item = BuiltInRegistries.ITEM.getValue(id);
        if (item == net.minecraft.world.item.Items.AIR && !id.toString().equals("minecraft:air")) { CommandManager.sendMessage("Unknown item: " + itemStr); return; }
        ItemStack stack = new ItemStack(item, count);
        // try to find slot
        Inventory inv = mc.player.getInventory();
        int slot = inv.getSlotWithRemainingSpace(stack);
        if (slot == -1) slot = inv.getFreeSlot();
        if (slot == -1) { CommandManager.sendMessage("No space in inventory"); return; }
        int before = inv.getItem(slot).getCount();
        int maxStack = inv.getMaxStackSize(inv.getItem(slot));
        // simplified: just add
        int added = Math.min(count, maxStack - before);
        if (inv.getItem(slot).isEmpty()) {
            inv.setItem(slot, stack);
            mc.gameMode.handleCreativeModeItemAdd(stack, inventoryToSlotId(slot));
        } else {
            ItemStack inSlot = inv.getItem(slot);
            inSlot.grow(added);
            mc.gameMode.handleCreativeModeItemAdd(inSlot, inventoryToSlotId(slot));
            remaining : {
                int remaining = count - added;
                if (remaining > 0) {
                    int free = inv.getFreeSlot();
                    if (free != -1) {
                        ItemStack remStack = new ItemStack(item, remaining);
                        inv.setItem(free, remStack);
                        mc.gameMode.handleCreativeModeItemAdd(remStack, inventoryToSlotId(free));
                    } else {
                        CommandManager.sendMessage("Partially gave " + added + "/" + count);
                        mc.player.inventoryMenu.broadcastChanges();
                        return;
                    }
                }
            }
        }
        mc.player.inventoryMenu.broadcastChanges();
        CommandManager.sendMessage("Gave " + count + " x " + stack.getDisplayName().getString());
    }

    private int inventoryToSlotId(int invId) {
        return invId < Inventory.SELECTION_SIZE ? InventoryMenu.USE_ROW_SLOT_START + invId : InventoryMenu.INV_SLOT_START - Inventory.SELECTION_SIZE + invId;
    }
}
