package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;

import java.util.List;

/**
 * Port of {@code crelog}. Reconnects to current server or singleplayer world.
 */
public final class RelogCommand extends Command {
    public static final RelogCommand INSTANCE = new RelogCommand();

    private RelogCommand() {
        super("relog", "Relog to server: .z relog (alias: crelog)");
    }

    @Override
    public List<String> suggest(String[] args) { return List.of(); }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { CommandManager.sendMessage("You must be in a world to relog"); return; }
        boolean ok = relog();
        if (!ok) CommandManager.sendMessage("Failed to relog");
        else CommandManager.sendMessage("Relogging...");
    }

    private boolean relog() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isLocalServer()) {
            IntegratedServer server = mc.getSingleplayerServer();
            if (server == null) return false;
            if (!disconnect()) return false;
            String levelName = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString();
            if (!mc.getLevelSource().levelExists(levelName)) return false;
            mc.createWorldOpenFlows().openWorld(levelName, () -> mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
            return true;
        } else {
            ServerData data = mc.getCurrentServer();
            if (data == null) return false;
            if (!disconnect()) return false;
            ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(data.ip), data, false, null);
            return true;
        }
    }

    private boolean disconnect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        boolean single = mc.isLocalServer();
        mc.level.disconnect(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        if (single) mc.disconnectWithSavingScreen();
        else mc.disconnectWithProgressScreen();
        mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        if (!single) mc.gui.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new net.minecraft.client.gui.screens.TitleScreen()));
        return true;
    }
}
