package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * The {@link BetterMovement} no-fall-damage helper. While {@code noFallTicks} is positive
 * (topped up each tick by {@link BetterMovement#tick}), outgoing move packets have their
 * {@code onGround} flag forced to true so the server never applies fall damage - but only
 * once the fall is significant (or, while elytra-flying, just before the player would touch
 * ground). Creative/spectator players and flyers are left alone. Called from the
 * movement {@code NoFall} mixin.
 */
public final class NoFall {
    /** Remaining ticks of no-fall protection; set by {@link BetterMovement}. */
    public static int noFallTicks = 0;
    private static final float MIN_FALL_DISTANCE = 2.5F;
    private static final double ELYTRA_LANDING_CHECK_DISTANCE = 0.6D;

    private NoFall() {
    }

    /** Optionally rewrites the packet's {@code onGround} flag before it is sent. */
    public static void onSendPacket(ServerboundMovePlayerPacket packet) {
        if (noFallTicks <= 0) {
            return;
        }

        noFallTicks--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (mc.player.getAbilities().flying) {
            return;
        }

        if (mc.player.isFallFlying()) {
            if (!isAboutToTouchGround(mc)) {
                return;
            }
        } else if (mc.player.fallDistance <= MIN_FALL_DISTANCE) {
            return;
        }

        ((com.zephyr.client.mixin.movement.NoFall.PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
    }

    /** Whether the elytra-flying player will soon collide with a block below them. */
    private static boolean isAboutToTouchGround(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return false;
        }

        if (mc.player.getDeltaMovement().y >= 0.0D) {
            return false;
        }

        return !mc.level.isUnobstructed(
                mc.player,
                Shapes.create(mc.player.getBoundingBox().move(0.0D, -ELYTRA_LANDING_CHECK_DISTANCE, 0.0D))
        );
    }
}
