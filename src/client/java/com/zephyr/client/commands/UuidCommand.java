package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;
import java.util.UUID;

/**
 * Port of {@code cuuid}.
 */
public final class UuidCommand extends Command {
    public static final UuidCommand INSTANCE = new UuidCommand();

    private UuidCommand() {
        super("uuid", "Get UUID: .z uuid <player|uuid> (alias: cuuid)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) return conn.getOnlinePlayers().stream().map(p -> p.getProfile().name()).toList();
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) { CommandManager.sendMessage("Not connected"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z uuid <player|uuid>"); return; }
        String target = args[0];
        UUID uuid = null;
        try { uuid = UUID.fromString(target); } catch (IllegalArgumentException ignored) {}
        if (uuid != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
            String uuidStr = uuid.toString();
            Component comp = Component.literal(uuidStr).withStyle(s -> s.withUnderlined(true).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))).withClickEvent(new ClickEvent.CopyToClipboard(uuidStr)));
            if (info == null) {
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal(uuidStr + " (no name) ").append(comp));
                CommandManager.sendMessage("UUID: " + uuidStr + " (no player name)");
            } else {
                CommandManager.sendMessage(info.getProfile().name() + " UUID: " + uuidStr);
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("UUID: ").append(comp));
            }
        } else {
            PlayerInfo info = mc.getConnection().getOnlinePlayers().stream().filter(p -> p.getProfile().name().equalsIgnoreCase(target)).findFirst().orElse(null);
            if (info == null) { CommandManager.sendMessage("Player not found: " + target); return; }
            String uuidStr = info.getProfile().id().toString();
            Component comp = Component.literal(uuidStr).withStyle(s -> s.withUnderlined(true).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))).withClickEvent(new ClickEvent.CopyToClipboard(uuidStr)));
            CommandManager.sendMessage(info.getProfile().name() + " UUID: " + uuidStr);
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("UUID: ").append(comp));
        }
    }
}
