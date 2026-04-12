package com.zephyr.client.module;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class F5Tweaks {
    public static boolean enabled = false;

    public static float cameraYaw = 0.0F;
    public static float cameraPitch = 0.0F;
    private static boolean cameraInitialized = false;

    public static boolean shouldFreeLook(Perspective perspective) {
        return enabled && perspective == Perspective.THIRD_PERSON_BACK;
    }

    public static void onPerspectiveChanged(Perspective perspective) {
        if (!shouldFreeLook(perspective)) {
            cameraInitialized = false;
        }
    }

    public static void reset(float playerYaw, float playerPitch) {
        cameraYaw = playerYaw;
        cameraPitch = playerPitch;
        cameraInitialized = true;
    }

    public static void ensureInitialized(float playerYaw, float playerPitch) {
        if (!cameraInitialized) {
            reset(playerYaw, playerPitch);
        }
    }

    public static void rotateCamera(float deltaYaw, float deltaPitch) {
        cameraYaw = MathHelper.wrapDegrees(cameraYaw + deltaYaw);
        cameraPitch = MathHelper.clamp(cameraPitch + deltaPitch, -90.0F, 90.0F);
    }
}
