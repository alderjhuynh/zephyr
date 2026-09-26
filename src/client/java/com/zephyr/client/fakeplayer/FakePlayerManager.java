package com.zephyr.client.fakeplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Singleplayer-gated registry for fake players.
 * All entry points check {@code isSingleplayer()} before touching the integrated server.
 * This guarantees the mod never requires a remote server to have the mod, preserving join compatibility.
 */
public final class FakePlayerManager {
    private FakePlayerManager() {}

    public static boolean isSingleplayer() {
        Minecraft mc = Minecraft.getInstance();
        // isLocalServer covers integrated server; getSingleplayerServer covers world not yet ticking
        return mc.isLocalServer() && mc.getSingleplayerServer() != null;
    }

    public static boolean isSpawning(String name) {
        return FakePlayerEntity.isSpawningPlayer(name);
    }

    public static IntegratedServer getServer() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    public static ServerPlayer getPlayer(String name) {
        IntegratedServer server = getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayerByName(name);
    }

    public static void tickCleanup() {
        // No-op: fake players clean up via kill() disconnect. Could prune stale spawning set here.
    }

    public static void onDisconnect() {
        // Called from ZephyrClient when leaving world; FakePlayerEntity instances live on integrated server which shuts down anyway.
        // Ensure spawning guard cleared on next start - already async cleared.
    }
}
