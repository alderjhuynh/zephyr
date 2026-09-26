package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;

/**
 * Port of {@code cping}.
 */
public final class PingCommand extends Command {
    public static final PingCommand INSTANCE = new PingCommand();

    private PingCommand() {
        super("ping", "Show ping: .z ping [player] (alias: cping)");
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
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) { CommandManager.sendMessage("Not connected"); return; }
        if (mc.isLocalServer()) { CommandManager.sendMessage("Singleplayer has no ping"); return; }
        if (args.length == 0) {
            int ping = getLocalPing();
            if (ping == -1) CommandManager.sendMessage("Could not get ping");
            else CommandManager.sendMessage("Ping: " + ping + "ms");
        } else {
            String name = args[0];
            PlayerInfo info = conn.getOnlinePlayers().stream().filter(p -> p.getProfile().name().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (info == null) { CommandManager.sendMessage("Player not found: " + name); return; }
            CommandManager.sendMessage(info.getProfile().name() + " ping: " + info.getLatency() + "ms");
        }
    }

    private int getLocalPing() {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        if (c == null) return -1;
        PlayerInfo p = c.getPlayerInfo(c.getLocalGameProfile().id());
        return p == null ? -1 : p.getLatency();
    }
}
