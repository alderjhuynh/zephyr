package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn {
    public static boolean enabled = true;

    public static void onScreenOpen(Object screen) {
        if (!enabled) return;

        if (screen instanceof DeathScreen) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                client.player.requestRespawn();
            }
        }
    }

    public static boolean enabled() {
        return enabled;
    }
}