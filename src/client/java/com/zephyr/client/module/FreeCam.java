package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FreeCam {
    public static boolean enabled = false;

    private static final double BASE_SPEED = 0.35D;
    private static final double SPRINT_MULTIPLIER = 2.5D;
    private static final FakePlayerMirror fakePlayerMirror = new FakePlayerMirror();

    private static double prevX;
    private static double prevY;
    private static double prevZ;
    private static double x;
    private static double y;
    private static double z;
    private static float yaw;
    private static float pitch;
    private static boolean initialized;

    private FreeCam() {
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }

        enabled = value;
        if (!enabled) {
            fakePlayerMirror.discard();
            initialized = false;
        }
    }

    public static void tick(MinecraftClient client) {
        if (!enabled) {
            fakePlayerMirror.discard();
            initialized = false;
            return;
        }

        if (client == null || client.player == null || client.world == null) {
            fakePlayerMirror.discard();
            initialized = false;
            return;
        }

        ensureInitialized(client.player);
        if (!fakePlayerMirror.isSpawned()) {
            fakePlayerMirror.spawn(client);
        }

        prevX = x;
        prevY = y;
        prevZ = z;
        fakePlayerMirror.syncIfNeeded(client.player);

        if (client.currentScreen != null) {
            return;
        }

        double forwardInput = getAxis(client.options.forwardKey.isPressed(), client.options.backKey.isPressed());
        double sidewaysInput = getAxis(client.options.leftKey.isPressed(), client.options.rightKey.isPressed());
        double verticalInput = getAxis(client.options.jumpKey.isPressed(), client.options.sneakKey.isPressed());

        Vec3d movement = getForwardVector().multiply(forwardInput)
                .add(getRightVector().multiply(sidewaysInput))
                .add(0.0D, verticalInput, 0.0D);

        if (movement.lengthSquared() < 1.0E-6D) {
            return;
        }

        double speed = BASE_SPEED;
        if (client.options.sprintKey.isPressed()) {
            speed *= SPRINT_MULTIPLIER;
        }

        movement = movement.normalize().multiply(speed);
        x += movement.x;
        y += movement.y;
        z += movement.z;
    }

    public static boolean isActive() {
        return enabled && initialized;
    }

    public static void ensureInitialized(ClientPlayerEntity player) {
        if (initialized) {
            return;
        }

        Vec3d cameraPos = player.getCameraPosVec(1.0F);
        prevX = cameraPos.x;
        prevY = cameraPos.y;
        prevZ = cameraPos.z;
        x = cameraPos.x;
        y = cameraPos.y;
        z = cameraPos.z;
        yaw = player.getYaw();
        pitch = player.getPitch();
        initialized = true;
    }

    public static void rotateCamera(float deltaYaw, float deltaPitch) {
        yaw = MathHelper.wrapDegrees(yaw + deltaYaw);
        pitch = MathHelper.clamp(pitch + deltaPitch, -90.0F, 90.0F);
    }

    public static double getX() {
        return x;
    }

    public static double getRenderX(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevX, x);
    }

    public static double getY() {
        return y;
    }

    public static double getRenderY(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevY, y);
    }

    public static double getZ() {
        return z;
    }

    public static double getRenderZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevZ, z);
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    private static double getAxis(boolean positivePressed, boolean negativePressed) {
        if (positivePressed == negativePressed) {
            return 0.0D;
        }

        return positivePressed ? 1.0D : -1.0D;
    }

    private static Vec3d getForwardVector() {
        double yawRadians = Math.toRadians(yaw);
        return new Vec3d(
                -Math.sin(yawRadians),
                0.0D,
                Math.cos(yawRadians)
        );
    }

    private static Vec3d getRightVector() {
        double yawRadians = Math.toRadians(yaw);
        return new Vec3d(
                Math.cos(yawRadians),
                0.0D,
                Math.sin(yawRadians)
        );
    }
}
