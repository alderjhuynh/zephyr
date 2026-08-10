package com.zephyr.client.module.qol.freecam;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A mutable snapshot of the camera's position and rotation, used to compute
 * the freecam placement. Movement is expressed in the camera's local
 * forward/up/right axes, which are derived from the yaw and pitch the same way
 * the vanilla camera computes its own axis planes.
 */
public class FreecamPosition {
    public double x;
    public double y;
    public double z;
    public float pitch;
    public float yaw;

    private final Quaternionf rotation = new Quaternionf(
            0.0F, 0.0F, 0.0F, 1.0F);
    private final Vector3f verticalPlane = new Vector3f(0.0F, 1.0F, 0.0F);
    private final Vector3f diagonalPlane = new Vector3f(1.0F, 0.0F, 0.0F);
    private final Vector3f horizontalPlane = new Vector3f(0.0F, 0.0F, 1.0F);

    /**
     * Captures the entity's current position and rotation, aligning the y to the
     * entity's eyes when it is not swimming.
     *
     * @param entity the entity to snapshot
     */
    public FreecamPosition(Entity entity) {
        x = entity.getX();
        y = getSwimmingY(entity);
        z = entity.getZ();
        setRotation(entity.getYRot(), entity.getXRot());
    }

    /**
     * Sets the yaw and pitch and recomputes the local axis planes from them.
     * Mirrors {@code net.minecraft.client.render.Camera.setRotation}.
     */
    public void setRotation(float yaw, float pitch) {
        this.pitch = pitch;
        this.yaw = yaw;

        rotation.rotationYXZ(-yaw * ((float) Math.PI / 180), pitch * ((float) Math.PI / 180), 0.0f);

        horizontalPlane.set(0.0f, 0.0f, 1.0f);
        verticalPlane.set(0.0f, 1.0f, 0.0f);
        diagonalPlane.set(1.0f, 0.0f, 0.0f);

        horizontalPlane.rotate(rotation);
        verticalPlane.rotate(rotation);
        diagonalPlane.rotate(rotation);
    }

    /**
     * Inverts the rotation so the camera looks back at its original direction,
     * matching the vanilla camera's mirrored third-person view.
     */
    public void mirrorRotation() {
        setRotation(yaw + 180.0F, -pitch);
    }

    /**
     * Moves forward (positive) or backward (negative) relative to the current
     * rotation.
     *
     * @param distance the distance to move along the forward axis
     */
    public void moveForward(double distance) {
        move(distance, 0, 0);
    }

    /**
     * Moves along the camera's local axes. Mirrors
     * {@code net.minecraft.client.render.Camera.moveBy}.
     *
     * @param fwd   distance along the forward axis
     * @param up    distance along the up axis
     * @param right distance along the right axis
     */
    public void move(double fwd, double up, double right) {
        x += (double) horizontalPlane.x() * fwd
           + (double) verticalPlane.x()   * up
           + (double) diagonalPlane.x()   * right;

        y += (double) horizontalPlane.y() * fwd
           + (double) verticalPlane.y()   * up
           + (double) diagonalPlane.y()   * right;

        z += (double) horizontalPlane.z() * fwd
           + (double) verticalPlane.z()   * up
           + (double) diagonalPlane.z()   * right;
    }

    /** The chunk containing this position. */
    public ChunkPos getChunkPos() {
        return new ChunkPos((int) (x / 16), (int) (z / 16));
    }

    private static double getSwimmingY(Entity entity) {
        if (entity.getPose() == Pose.SWIMMING) {
            return entity.getY();
        }
        return entity.getY() - entity.getEyeHeight(Pose.SWIMMING) + entity.getEyeHeight(entity.getPose());
    }
}
