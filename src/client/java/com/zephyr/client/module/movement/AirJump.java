package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Movement module that lets the player jump while airborne. Pressing the jump
 * key mid-air triggers a jump from ground, once per key press (edge-triggered),
 * so repeated jumps act as an air jump or double-jump. The edge state is reset
 * whenever the player returns to a surface.
 */
public final class AirJump extends Module {
    public static final AirJump INSTANCE = new AirJump();
    private AirJump() {
        super("AirJump", "Allows jumping in the air", Category.MOVEMENT);
    }

    private static boolean wasJumpPressed = false;

    /**
     * Performs the air jump each tick: while airborne, a fresh jump-key press
     * calls {@link LocalPlayer#jumpFromGround()}. Grounded, climbing, swimming
     * and creative-flying states clear the pending edge.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        if (player.onGround() || player.onClimbable() || player.isInLiquid() || player.getAbilities().flying) {
            wasJumpPressed = false;
            return;
        }

        boolean isJumpPressed = client.options.keyJump.isDown();

        if (isJumpPressed && !wasJumpPressed) {
            player.jumpFromGround();
        }

        wasJumpPressed = isJumpPressed;
    }

    /** Resets the jump edge state, used when the module is (re)enabled. */
    public static void onEnable(Minecraft client) {
        wasJumpPressed = false;
    }
}
