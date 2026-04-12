package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;

public class Sneak {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = false;

    public static void onTick() {
        if (!enabled) return;
        if (mc.player == null) return;
        mc.options.sneakKey.setPressed(enabled);
    }
}