package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

import java.util.List;

/**
 * Port of {@code cpermissionlevel}.
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
        PermissionSet perms = mc.player.permissions();
        PermissionLevel[] levels = PermissionLevel.values();
        int lvl = PermissionLevel.ALL.id();
        for (int i = levels.length-1; i >=0; i--) {
            if (perms.hasPermission(new Permission.HasCommandLevel(levels[i]))) { lvl = levels[i].id(); break; }
        }
        CommandManager.sendMessage("Permission level: " + lvl);
    }
}
