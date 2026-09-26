package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shared fluid-state checks used by the {@link BetterMovement} helpers to suspend their
 * special movement in water/lava (dashes, double jumps, wave dashes and glides all refuse
 * to run while the player is submerged).
 */
public final class FluidCheck {
    private static final Minecraft client = Minecraft.getInstance();

    /** The current local player, or {@code null} outside a world. */
    private static LocalPlayer player() {
        return client.player;
    }

    /** Whether the player is inside a water body. */
    public static boolean isTouchingWater() {
        LocalPlayer p = player();
        return p != null && p.isInWater();
    }

    /** Whether the player is inside a water body. */
    public static boolean isInWater() {
        LocalPlayer p = player();
        return p != null && p.isInWater();
    }

    /** Whether the player is inside lava. */
    public static boolean isInLava() {
        LocalPlayer p = player();
        return p != null && p.isInLava();
    }

    /** Whether the player's eyes are submerged (fully underwater). */
    public static boolean isEyeInFluid() {
        LocalPlayer p = player();
        return p != null && p.isUnderWater();
    }

    /** Convenience check combining all fluid conditions; false when no player is present. */
    public static boolean anyCheck() {
        return isTouchingWater()
                || isInWater()
                || isInLava()
                || isEyeInFluid();
    }
}
