package com.zephyr.client.module.qol.freecam;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Computes the freecam velocity for the default flight mode. It resolves the
 * camera's forward and side vectors from its yaw, combines the movement keys
 * (optionally sprinting faster), normalizes diagonal movement, and applies the
 * vertical speed through the jump and sneak keys.
 */
public class Motion {

    /** Factor applied to horizontal velocity when moving diagonally, equal to {@code sin(45°)}. */
    public static final double DIAGONAL_MULTIPLIER = Mth.sin((float) Math.toRadians(45));

    /**
     * Sets the freecam camera's velocity for this tick based on its input.
     *
     * @param freeCamera the camera entity to move
     * @param hSpeed     the configured horizontal speed
     * @param vSpeed     the configured vertical speed
     */
    public static void doMotion(FreeCamera freeCamera, double hSpeed, double vSpeed) {
        float yaw = freeCamera.getYRot();
        double velocityX = 0.0;
        double velocityY = 0.0;
        double velocityZ = 0.0;

        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90);

        hSpeed = hSpeed * (freeCamera.isSprinting() ? 1.5 : 1.0);

        boolean straight = false;
        if (freeCamera.input.keyPresses.forward()) {
            velocityX += forward.x * hSpeed;
            velocityZ += forward.z * hSpeed;
            straight = true;
        }
        if (freeCamera.input.keyPresses.backward()) {
            velocityX -= forward.x * hSpeed;
            velocityZ -= forward.z * hSpeed;
            straight = true;
        }

        boolean strafing = false;
        if (freeCamera.input.keyPresses.right()) {
            velocityZ += side.z * hSpeed;
            velocityX += side.x * hSpeed;
            strafing = true;
        }
        if (freeCamera.input.keyPresses.left()) {
            velocityZ -= side.z * hSpeed;
            velocityX -= side.x * hSpeed;
            strafing = true;
        }

        if (straight && strafing) {
            velocityX *= DIAGONAL_MULTIPLIER;
            velocityZ *= DIAGONAL_MULTIPLIER;
        }

        if (freeCamera.input.keyPresses.jump()) {
            velocityY += vSpeed;
        }
        if (freeCamera.input.keyPresses.shift()) {
            velocityY -= vSpeed;
        }

        freeCamera.setDeltaMovement(velocityX, velocityY, velocityZ);
    }
}
