package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AirJump extends Module {
    public static final AirJump INSTANCE = new AirJump();
    private AirJump() {
        super("AirJump", "Allows jumping in the air", Category.MOVEMENT);
    }

    private static boolean wasJumpPressed = false;

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

    public static void onEnable(Minecraft client) {
        wasJumpPressed = false;
    }
}
