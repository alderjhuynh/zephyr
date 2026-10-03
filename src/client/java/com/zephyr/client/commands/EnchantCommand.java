package com.zephyr.client.commands;

import java.util.List;

/**
 * Port of {@code cenchant}. Requires EnchantmentCracker which is not present in Zephyr.
 * Provides stub that explains limitation and guides to manual enchanting.
 */
public final class EnchantCommand extends Command {
    public static final EnchantCommand INSTANCE = new EnchantCommand();

    private EnchantCommand() {
        super("enchant", "Predict/crack enchantments (stub): .z enchant <item> ... (alias: cenchant)");
    }

    @Override
    public List<String> suggest(String[] args) {
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z enchant <item> [enchantments...] [--simulate]");
            CommandManager.sendMessage("Note: Enchantment cracking is not ported in Zephyr. This is a stub.");
            return;
        }
        CommandManager.sendMessage("cenchant not fully ported: requires EnchantmentCracker + PlayerRandCracker. Use vanilla enchanting or install ClientCommands for full feature.");
    }
}
