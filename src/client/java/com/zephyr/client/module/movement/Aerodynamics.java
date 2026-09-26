package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Movement module that boosts the player's velocity while sprinting (or while
 * elytra-flying with sprint held) in the air, accelerating them along their look
 * direction each tick. The strength is controlled by an {@code Acceleration}
 * setting. Movement behavior is disabled while on the ground.
 */
public final class Aerodynamics extends Module {
    public static final Aerodynamics INSTANCE = new Aerodynamics();

    private final NumberSetting acceleration =
            new NumberSetting("Acceleration", 0.04D, 0.00D, 2.0D, 0.01D);

    private Aerodynamics() {
        super("Aerodynamics", "Boosts velocity while sprinting", Category.MOVEMENT);
        addSetting(acceleration);
    }

    /**
     * Applies the acceleration impulse to the player each tick when airborne and
     * boosting (sprinting, or fall-flying with the sprint key held).
     *
     * @param client the Minecraft client instance
     */
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
