package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.mixin.movement.NoFall.PlayerMoveC2SPacketAccessor;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Movement module that prevents fall damage by rewriting outgoing move packets
 * so the server believes the player is always on the ground. A connection mixin
 * calls {@link #onSendPacket} for each {@link ServerboundMovePlayerPacket} before
 * it is sent, setting its on-ground flag to true while the module is enabled.
 */
public final class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();

    private NoFall() {
        super("No Fall", "Prevents fall damage packets", Category.MOVEMENT);
    }

    /**
     * Forces the on-ground flag of the outgoing move packet to true while the
     * module is enabled, so the server never registers fall distance.
     *
     * @param packet the outgoing move packet to rewrite
     */
    public static void onSendPacket(ServerboundMovePlayerPacket packet) {
        if (INSTANCE.isEnabled()) {
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
        }
    }
}
