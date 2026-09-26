package com.zephyr.client.mixin.movement.NoFall;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin targeting {@link net.minecraft.network.protocol.game.ServerboundMovePlayerPacket}.
 * Exposes a mutable setter for the private {@code onGround} field so the No Fall
 * and Better Movement no-fall logic can rewrite the on-ground state of outgoing
 * move packets.
 */
@Mixin(ServerboundMovePlayerPacket.class)
public interface PlayerMoveC2SPacketAccessor {

    /** Sets the packet's {@code onGround} field to the given value. */
    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);
}
