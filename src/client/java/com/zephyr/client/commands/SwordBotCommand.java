package com.zephyr.client.commands;

import com.zephyr.client.module.bot.SwordBot;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * The {@code .z swordbot <...>} command: runtime controls for the
 * {@link SwordBot} module that mirror swordbot-v3's keybinds without
 * consuming raw keys. {@code on/off} toggles the module, {@code retarget}
 * re-acquires the nearest target, {@code skill/style} cycle those settings,
 * {@code nn} flips the neural-net toggle, and {@code status} prints the
 * engine status line.
 */
public final class SwordBotCommand extends Command {

    public static final SwordBotCommand INSTANCE = new SwordBotCommand();

    private SwordBotCommand() {
        super("swordbot", "Controls the SwordBot: .z swordbot <on|off|retarget|skill|style|nn|status>");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length <= 1) {
            return List.of("on", "off", "retarget", "skill", "style", "nn", "status");
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft client = Minecraft.getInstance();
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z swordbot <on|off|retarget|skill|style|nn|status>");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "on" -> {
                SwordBot.INSTANCE.setEnabled(true);
                CommandManager.sendMessage(SwordBot.INSTANCE.engine().statusLine());
            }
            case "off" -> SwordBot.INSTANCE.setEnabled(false);
            case "retarget" -> SwordBot.INSTANCE.retarget(client);
            case "skill" -> SwordBot.INSTANCE.cycleSkill();
            case "style" -> SwordBot.INSTANCE.cycleStyle();
            case "nn" -> SwordBot.INSTANCE.toggleNn(client);
            case "status" -> CommandManager.sendMessage(SwordBot.INSTANCE.engine().statusLine());
            default -> CommandManager.sendMessage("Usage: .z swordbot <on|off|retarget|skill|style|nn|status>");
        }
    }
}
