package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Aerodynamics extends Module {
    public static final Aerodynamics INSTANCE = new Aerodynamics();

    private final NumberSetting acceleration =
            new NumberSetting("Acceleration", 0.04D, 0.00D, 2.0D, 0.01D);

    private Aerodynamics() {
        super("Aerodynamics", "Boosts velocity while sprinting", Category.MOVEMENT);
        addSetting(acceleration);
    }

    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.onGround()) {
            return;
        }

        boolean boosting = player.isSprinting()
                || (player.isFallFlying() && client.options.keySprint.isDown());
        if (!boosting) {
            return;
        }

        Vec3 direction = getBoostDirection(player);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        player.addDeltaMovement(direction.scale(acceleration.get()));
    }

    private static Vec3 getBoostDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle();
        if (lookDirection.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return lookDirection.normalize();
    }
}
