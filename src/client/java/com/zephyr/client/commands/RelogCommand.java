package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;

/**
 * Port of {@code crelog}. Reconnects to current server or singleplayer world.
 *
 * 1.21.1 port: mc.screen field + mc.setScreen (no mc.gui.screen()/mc.gui.setScreen),
 * ClientLevel.disconnect() takes no args, mc.disconnect() replaces
 * disconnectWithSavingScreen/disconnectWithProgressScreen.
 * ConnectScreen.startConnecting signature is identical in 1.21.1.
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
            String levelName = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
            if (!mc.getLevelSource().levelExists(levelName)) return false;
            mc.createWorldOpenFlows().openWorld(levelName, () -> mc.setScreen(new TitleScreen()));
            return true;
        } else {
            ServerData data = mc.getCurrentServer();
            if (data == null) return false;
            if (!disconnect()) return false;
            ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(data.ip), data, false, null);
            return true;
        }
    }

    private boolean disconnect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        boolean single = mc.isLocalServer();
        mc.level.disconnect();
        mc.disconnect();
        mc.setScreen(new TitleScreen());
        if (!single) mc.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
        return true;
    }
}
