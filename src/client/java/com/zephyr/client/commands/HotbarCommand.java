package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Port of {@code chotbar}. Saves/restores hotbars via HotbarManager.
 */
public final class HotbarCommand extends Command {
    public static final HotbarCommand INSTANCE = new HotbarCommand();

    private HotbarCommand() {
        super("hotbar", "Hotbar save/restore: .z hotbar <save|restore> <1-9> (alias: chotbar)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("save", "restore");
        if (args.length == 2) return List.of("1","2","3","4","5","6","7","8","9");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length < 2) { CommandManager.sendMessage("Usage: .z hotbar <save|restore> <1-9>"); return; }
        String op = args[0].toLowerCase();
        int idx;
        try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException e) { CommandManager.sendMessage("Index must be 1-9"); return; }
        if (idx < 1 || idx > 9) { CommandManager.sendMessage("Index must be 1-9"); return; }
        HotbarManager mgr = mc.getHotbarManager();
        if (op.equals("save")) {
            Hotbar hotbar = mgr.get(idx - 1);
            hotbar.storeFrom(mc.player.getInventory(), mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess());
            mgr.save();
            CommandManager.sendMessage("Saved hotbar " + idx);
        } else if (op.equals("restore")) {
            if (!mc.player.isCreative() && !mc.player.getAbilities().instabuild) { CommandManager.sendMessage("You must be in creative to restore"); return; }
            Hotbar hotbar = mgr.get(idx - 1);
            var access = mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
            List<ItemStack> items = hotbar.load(access);
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                ItemStack stack = items.get(slot);
                mc.player.getInventory().setItem(slot, stack);
                mc.gameMode.handleCreativeModeItemAdd(stack, 36 + slot);
            }
            mc.player.inventoryMenu.broadcastChanges();
            CommandManager.sendMessage("Restored hotbar " + idx);
        } else {
            CommandManager.sendMessage("Usage: .z hotbar <save|restore> <1-9>");
        }
    }
}
