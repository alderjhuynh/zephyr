package com.zephyr.client.module.qol.freecam;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec2;
import com.zephyr.client.module.qol.FreeCam;

import java.util.UUID;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * The detached camera entity used by FreeCam. It is a fake client-side player
 * added to the level while freecam is active; the client's camera is retargeted
 * to it so the view can fly freely while the real player stays in place.
 *
 * <p>It borrows the real player's hand-swing/use animations and effects so the
 * view looks natural, and overrides water/ladder/piston/collision behavior so
 * the camera never gets slowed or moved by the world unless collisions are
 * explicitly checked at spawn.
 */
public class FreeCamera extends AbstractClientPlayer {
    public ClientInput input;
    public float yBob;
    public float xBob;
    public float yBobO;
    public float xBobO;

    /**
     * Creates the camera with a random profile name, forced swimming pose, and
     * enabled flying abilities.
     *
     * @param id the entity id to give the camera
     */
    public FreeCamera(int id) {
        super(MC.level, new GameProfile(UUID.randomUUID(), "FreeCamera"));

        setId(id);
        setPose(Pose.SWIMMING);
        getAbilities().flying = true;
        input = new KeyboardInput(MC.options);
    }

    /** Polls the camera's input and applies movement for this tick. */
    @Override
    public void tick() {
        input.tick();
        doMotion();
        super.tick();
    }

    /** Copies the given entity's position and rotation into the camera. */
    @Override
    public void copyPosition(Entity entity) {
        applyPosition(new FreecamPosition(entity));
    }

    /**
     * Snapshots the camera's position and rotation, resetting the view-bob state
     * so the camera does not rotate when freecam is first entered.
     */
    public void applyPosition(FreecamPosition position) {
        snapTo(position.x, position.y, position.z, position.yaw, position.pitch);
        xBob = getXRot();
        yBob = getYRot();
        xBobO = xBob; // Prevents camera from rotating upon entering freecam.
        yBobO = yBob;
    }

    /**
     * Repositions the camera according to the given start perspective, moving
     * forward (or backward for third-person views) from the player. When
     * {@code checkCollision} is true the camera stops before it would collide.
     */
    public void applyPerspective(Perspective perspective, boolean checkCollision) {
        FreecamPosition position = new FreecamPosition(this);

        switch (perspective) {
            case INSIDE:
                // No-op
                break;
            case FIRST_PERSON:
                // Move just in front of the player's eyes
                moveForwardUntilCollision(position, 0.4, checkCollision);
                break;
            case THIRD_PERSON_MIRROR:
                // Invert the rotation and fallthrough into the THIRD_PERSON case
                position.mirrorRotation();
            case THIRD_PERSON:
                // Move back as per F5 mode
                moveForwardUntilCollision(position, -4.0, checkCollision);
                break;
        }
    }

    // Move FreeCamera forward using FreecamPosition.moveForward.
    // If checkCollision is true, stop moving forward before hitting a collision.
    // Return true if successfully able to move.
    private boolean moveForwardUntilCollision(FreecamPosition position, double distance, boolean checkCollision) {
        if (!checkCollision) {
            position.moveForward(distance);
            applyPosition(position);
            return true;
        }
        return moveForwardUntilCollision(position, distance);
    }

    // Same as above, but always check collision.
    private boolean moveForwardUntilCollision(FreecamPosition position, double maxDistance) {
        boolean negative = maxDistance < 0;
        maxDistance = negative ? -1 * maxDistance : maxDistance;
        double increment = 0.1;

        // Move forward by increment until we reach maxDistance or hit a collision
        for (double distance = 0.0; distance < maxDistance; distance += increment) {
            FreecamPosition oldPosition = new FreecamPosition(this);

            position.moveForward(negative ? -1 * increment : increment);
            applyPosition(position);

            if (!wouldNotSuffocateAtTargetPose(getPose())) {
                // Revert to last non-colliding position and return whether we were unable to move at all
                applyPosition(oldPosition);
                return distance > 0;
            }
        }

        return true;
    }

    private ClientLevel getClientLevel() {
        return (ClientLevel) level();
    }

    /** Adds the camera entity to the client level. */
    public void spawn() {
        getClientLevel().addEntity(this);
    }

    /** Removes the camera entity from the client level. */
    public void despawn() {
        if (level() != null) {
            getClientLevel().removeEntity(getId(), RemovalReason.DISCARDED);
        }
    }

    /** Prevents fall damage sound when FreeCamera touches ground with noClip disabled. */
    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    /** Forwards the real player's attack animation so hand swings show in freecam. */
    @Override
    public float getAttackAnim(float tickDelta) {
        return MC.player.getAttackAnim(tickDelta);
    }

    /** Forwards the real player's use-item timer so use animations show in freecam. */
    @Override
    public int getUseItemRemainingTicks() {
        return MC.player.getUseItemRemainingTicks();
    }

    /** Forwards the real player's using-item state for use animations in freecam. */
    @Override
    public boolean isUsingItem() {
        return MC.player.isUsingItem();
    }

    /** Prevents slow down from ladders/vines. */
    @Override
    public boolean onClimbable() {
        return false;
    }

    /** Prevents slow down from water. */
    @Override
    public boolean isInWater() {
        return false;
    }

    /** Forwards the real player's effects so night vision applies to the camera (e.g. with Iris). */
    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return MC.player.getEffect(effect);
    }

    /** Prevents pistons from moving FreeCamera when collision.ignoreAll is enabled. */
    @Override
    public PushReaction getPistonPushReaction() {
        return FreeCam.ignoreCollision() ? PushReaction.IGNORE : PushReaction.NORMAL;
    }

    /** Prevents collision with solid entities (shulkers, boats). */
    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    /** Ensures that the FreeCamera is always in the swimming pose. */
    @Override
    public void setPose(Pose pose) {
        super.setPose(Pose.SWIMMING);
    }

    /** Tracks submersion in water without playing the submersion sound. */
    @Override
    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
        return this.wasUnderwater;
    }

    /** Prevents water submersion sounds from playing. */
    @Override
    protected void doWaterSplashEffect() {}

    private void doMotion() {
        switch (FreeCam.getFlightMode()) {
            case DEFAULT -> {
                getAbilities().setFlyingSpeed(0);
                Motion.doMotion(this, FreeCam.getHorizontalSpeed(), FreeCam.getVerticalSpeed());
            }
            case CREATIVE -> {
                getAbilities().setFlyingSpeed((float) FreeCam.getVerticalSpeed() / 10);

                if (this.input.keyPresses.shift() ^ this.input.keyPresses.jump()) {
                    int direction = this.input.keyPresses.jump() ? 1 : -1;
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0F, ((float) direction * this.getAbilities().getFlyingSpeed() * 3.0F), 0.0F));
                }
            }
        }
        getAbilities().flying = true;
        setOnGround(false);
    }

    /** Returns the camera's own pitch for view rendering. */
    @Override
    public float getViewXRot(float partialTick) {
        return this.getXRot();
    }

    /** Returns the camera's own yaw for view rendering. */
    @Override
    public float getViewYRot(float partialTick) {
        return this.getYRot();
    }

    /** Enables AI-style ticking so the camera receives movement updates. */
    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    /** Lets the camera's movement simulation (travel) run each tick. */
    @Override
    public boolean canSimulateMovement() {
        return true;
    }

    /** Applies the camera's input vector, damping diagonal movement and updating the view-bob state. */
    @Override
    protected void applyInput() {
        Vec2 vec2 = this.input.getMoveVector();
        if (vec2.lengthSquared() != 0.0F)
            vec2 = vec2.scale(0.98F);
        applyInputHelper(vec2, this.input.keyPresses.jump());
    }

    private void applyInputHelper(Vec2 moveVector, boolean jumping) {
        this.xxa = moveVector.x;
        this.zza = moveVector.y;
        this.jumping = jumping;
        this.setSprinting((MC.options.keySprint.isDown() && this.input.keyPresses.forward()) || (this.input.keyPresses.forward() && this.isSprinting()));
        this.yBobO = this.yBob;
        this.xBobO = this.xBob;
        this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
        this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
    }
}
