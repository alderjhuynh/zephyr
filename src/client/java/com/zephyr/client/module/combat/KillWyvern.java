package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class KillWyvern extends Module {
    public static String targetUUID = "d4d73b2e-16fc-42b3-a292-f958cee33785";
    public static String selfUUID = "1176ac92-5aec-4761-96ab-6ead488f8bb5";
    public static final KillWyvern INSTANCE = new KillWyvern();

    private KillWyvern() {
        super("Kill Wyvern", "'Say hello' to a specific player", Category.COMBAT);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!client.player.getStringUUID().equals(selfUUID)) return;

        AbstractClientPlayer target = findPlayerByUUID(client, targetUUID);
        if (target != null && isWithinRenderDistance(client, target)) {

        }
    }

    private AbstractClientPlayer findPlayerByUUID(Minecraft client, String uuidStr) {
        for (AbstractClientPlayer player : client.level.players()) {
            if (player.getStringUUID().equals(uuidStr)) {
                return player;
            }
        }
        return null;
    }

    private boolean isWithinRenderDistance(Minecraft client, AbstractClientPlayer entity) {
        double renderDistanceBlocks = client.options.renderDistance().get() * 16.0;
        double distance = client.player.distanceTo(entity);
        return distance <= renderDistanceBlocks;
    }
}
