package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FluidCheck {
    private static final Minecraft client = Minecraft.getInstance();

    private static LocalPlayer player() {
        return client.player;
    }

    public static boolean isTouchingWater() {
        LocalPlayer p = player();
        return p != null && p.isInWater();
    }

    public static boolean isInWater() {
        LocalPlayer p = player();
        return p != null && p.isInWater();
    }

    public static boolean isInLava() {
        LocalPlayer p = player();
        return p != null && p.isInLava();
    }

    public static boolean isEyeInFluid() {
        LocalPlayer p = player();
        return p != null && p.isUnderWater();
    }

    public static boolean anyCheck() {
        return isTouchingWater()
                || isInWater()
                || isInLava()
                || isEyeInFluid();
    }
}
