package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Port of {@code ccrackrng}. Original relied on PlayerRandCracker / CCrackRng.
 * Zephyr has no player RNG cracker; this stub integrates with Seedcracker where possible
 * and otherwise informs the user.
 */
public final class CrackRngCommand extends Command {
    public static final CrackRngCommand INSTANCE = new CrackRngCommand();

    private CrackRngCommand() {
        super("crackrng", "Crack player RNG (port stub): .z crackrng (alias: ccrackrng)");
    }

    @Override
    public List<String> suggest(String[] args) { return List.of(); }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            CommandManager.sendMessage("You must be in a world to crack RNG");
            return;
        }
        CommandManager.sendMessage("ccrackrng: Player RNG cracking is not fully ported in Zephyr. Use .z seedcracker for world-seed cracking.");
        CommandManager.sendMessage("If you need player RNG, enable fishing/enchant prediction via modules and use the Seedcracker directly.");
    }
}
