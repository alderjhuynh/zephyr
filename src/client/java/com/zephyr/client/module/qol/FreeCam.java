package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Detaches the camera from the player so it can fly around freely while the
 * player's body stays in place. Look and movement are driven per-frame by the
 * {@code Camera} mixin; mouse look is redirected here by the {@code Entity.turn}
 * mixin, and the {@code KeyboardInput} mixin freezes the real player's input.
 */
public final class FreeCam extends Module {
    public static final FreeCam INSTANCE = new FreeCam();

    /** Matches the yaw/pitch multiplier Minecraft applies inside {@code Entity.turn}. */
    private static final double MOUSE_SENSITIVITY_FACTOR = 0.15;

    private final NumberSetting speed = new NumberSetting("Speed", 5.0, 0.5, 30.0, 0.5);
    private final BooleanSetting showPlayer = new BooleanSetting("Show Player", true);

    private Vec3 pos;
    private float yaw;
    private float pitch;

    private FreeCam() {
        super("FreeCam", "Detaches the camera to fly freely while your player stays in place", Category.QOL);
        addSetting(speed);
        addSetting(showPlayer);
    }

    @Override
    protected void onEnable() {
        initialize();
    }

    @Override
    protected void onDisable() {
        pos = null;
    }

    /**
     * Initializes the camera from the player's current eye position/rotation.
     * Called on enable, and lazily from the camera mixin to cover modules that
     * are toggled on before a world (and therefore a player) exists.
     */
    public void initialize() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        pos = client.player.getEyePosition();
        yaw = client.player.getViewYRot(1.0F);
        pitch = client.player.getViewXRot(1.0F);
    }

    /**
     * Moves the camera based on the movement keys, called every rendered frame so
     * motion is smooth and proportional to real time rather than the 20 TPS tick.
     */
    public void onCameraUpdate(DeltaTracker deltaTracker) {
        if (pos == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.screen() != null) return;

        float deltaTicks = Math.min(deltaTracker.getRealtimeDeltaTicks(), 1.0F);
        float distance = speed.get().floatValue() * (deltaTicks / 20.0F);

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        Vec3 movement = Vec3.ZERO;
        if (client.options.keyUp.isDown()) {
            movement = movement.add(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad));
        }
        if (client.options.keyDown.isDown()) {
            movement = movement.subtract(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad));
        }
        if (client.options.keyRight.isDown()) {
            movement = movement.add(-Math.cos(yawRad), 0, Math.sin(yawRad));
        }
        if (client.options.keyLeft.isDown()) {
            movement = movement.subtract(-Math.cos(yawRad), 0, Math.sin(yawRad));
        }
        if (client.options.keyJump.isDown()) {
            movement = movement.add(0, 1, 0);
        }
        if (client.options.keyShift.isDown()) {
            movement = movement.subtract(0, 1, 0);
        }

        if (movement.lengthSqr() > 1.0E-4) {
            movement = movement.normalize().scale(distance);
        }
        pos = pos.add(movement);
    }

    /** Applies camera look deltas (already sensitivity-scaled by the mouse handler). */
    public void onLook(double yawDelta, double pitchDelta) {
        yaw = Mth.wrapDegrees(yaw + (float) yawDelta * (float) MOUSE_SENSITIVITY_FACTOR);
        pitch = Mth.clamp(pitch + (float) pitchDelta * (float) MOUSE_SENSITIVITY_FACTOR, -90.0F, 90.0F);
    }

    public Vec3 getPos() {
        return pos;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean shouldShowPlayer() {
        return showPlayer.get();
    }
}
