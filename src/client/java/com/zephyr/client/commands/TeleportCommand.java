package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;

import java.util.List;
import java.util.UUID;

/**
 * Port of {@code ctp}. Spectator teleport via packet.
 */
public final class TeleportCommand extends Command {
    public static final TeleportCommand INSTANCE = new TeleportCommand();

    private TeleportCommand() {
        super("tp", "Spectator teleport: .z tp <player|uuid> (alias: ctp)");
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
        if (mc.player == null || mc.getConnection() == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (!mc.player.isSpectator()) { CommandManager.sendMessage("You must be in spectator mode"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z tp <player|uuid>"); return; }
        String target = args[0];
        UUID uuid = null;
        String name = target;
        // try parse uuid
        try { uuid = UUID.fromString(target); } catch (IllegalArgumentException ignored) {}
        if (uuid == null) {
            // lookup by name
            PlayerInfo info = mc.getConnection().getOnlinePlayers().stream().filter(p -> p.getProfile().name().equalsIgnoreCase(target)).findFirst().orElse(null);
            if (info == null) { CommandManager.sendMessage("Player not found: " + target); return; }
            uuid = info.getProfile().id();
            name = info.getProfile().name();
        } else {
            PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) name = info.getProfile().name();
        }
        mc.getConnection().send(new ServerboundTeleportToEntityPacket(uuid));
        CommandManager.sendMessage("Teleporting to " + name);
    }
}
