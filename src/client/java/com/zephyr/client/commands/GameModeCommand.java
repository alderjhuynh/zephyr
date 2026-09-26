package com.zephyr.client.commands;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Port of {@code cgamemode}. Supports query <player> and list <gamemode>.
 */
public final class GameModeCommand extends Command {
    public static final GameModeCommand INSTANCE = new GameModeCommand();

    private GameModeCommand() {
        super("gamemode", "Query gamemodes: .z gamemode <query <player>|list <gamemode>> (alias: cgamemode)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("query", "list");
        if (args.length == 2 && args[0].equalsIgnoreCase("query")) {
            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) return conn.getOnlinePlayers().stream().map(p -> p.getProfile().name()).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return List.of("survival", "creative", "adventure", "spectator");
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) { CommandManager.sendMessage("Not connected"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z gamemode <query <player>|list <gamemode>>"); return; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("query")) {
            if (args.length < 2) { CommandManager.sendMessage("Usage: .z gamemode query <player>"); return; }
            String name = args[1];
            PlayerInfo info = mc.getConnection().getOnlinePlayers().stream().filter(p -> p.getProfile().name().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (info == null) {
                // try by uuid resolution via player list
                info = mc.getConnection().getPlayerInfo(java.util.UUID.nameUUIDFromBytes(name.getBytes()));
                if (info == null) { CommandManager.sendMessage("Player not found: " + name); return; }
            }
            GameType gm = info.getGameMode();
            String gmName = gm == null ? "unknown" : gm.getName();
            CommandManager.sendMessage(name + " gamemode: " + gmName);
        } else if (sub.equals("list")) {
            if (args.length < 2) { CommandManager.sendMessage("Usage: .z gamemode list <survival|creative|adventure|spectator>"); return; }
            GameType target = parse(args[1]);
            if (target == null) { CommandManager.sendMessage("Unknown gamemode: " + args[1]); return; }
            var matching = mc.getConnection().getOnlinePlayers().stream().filter(p -> p.getGameMode() == target).collect(Collectors.toList());
            if (matching.isEmpty()) CommandManager.sendMessage("No players in " + target.getName());
            else {
                CommandManager.sendMessage("Players in " + target.getName() + ":");
                matching.forEach(p -> CommandManager.sendMessage("- " + p.getProfile().name()));
            }
        } else {
            CommandManager.sendMessage("Usage: .z gamemode <query <player>|list <gamemode>>");
        }
    }

    private GameType parse(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "survival", "0", "s" -> GameType.SURVIVAL;
            case "creative", "1", "c" -> GameType.CREATIVE;
            case "adventure", "2", "a" -> GameType.ADVENTURE;
            case "spectator", "3", "sp" -> GameType.SPECTATOR;
            default -> null;
        };
    }
}
