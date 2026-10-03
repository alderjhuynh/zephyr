package com.zephyr.client.commands;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Port of {@code cpermissionlevel}.
 *
 * 1.21.1 port: the 26.x net.minecraft.server.permissions package
 * (Permission/PermissionLevel/PermissionSet) and player.permissions() do not
 * exist here, so there is no numeric permission level to query. In
 * singleplayer we report op status via PlayerList.isOp; on multiplayer the
 * server does not expose this client-side.
 */
public final class PermissionLevelCommand extends Command {
    public static final PermissionLevelCommand INSTANCE = new PermissionLevelCommand();

    private PermissionLevelCommand() {
        super("permissionlevel", "Show permission level: .z permissionlevel (alias: cpermissionlevel)");
    }

    @Override
    public List<String> suggest(String[] args) { return List.of(); }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (mc.getSingleplayerServer() == null) {
            CommandManager.sendMessage("Permission level can't be checked client-side on multiplayer");
            return;
        }
        GameProfile profile = new GameProfile(mc.getUser().getProfileId(), mc.getUser().getName());
        boolean op = mc.getSingleplayerServer().getPlayerList().isOp(profile);
        // Preserve the original "Permission level: N" shape: op ~ 4, non-op ~ 0.
        CommandManager.sendMessage("Permission level: " + (op ? "4 (op)" : "0 (not op)"));
    }
}
