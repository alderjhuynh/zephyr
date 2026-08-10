package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.NoFall;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link net.minecraft.network.Connection}. Injects at the head
 * of the {@code send(Packet, ChannelFutureListener, boolean)} overload to feed
 * outgoing move packets to the Better Movement no-fall logic, which rewrites the
 * packet's on-ground flag when a no-fall is pending and the player is about to
 * take fall damage. Backs the Better Movement {@code NoFall} feature.
 */
@Mixin(Connection.class)
public class NoFallConnectionMixin {
    /** Forwards every outgoing move packet to {@code NoFall.onSendPacket}. */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD")
    )
    private void zephyr$betterMovementNoFall(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (!BetterMovement.enabled) return;
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            NoFall.onSendPacket(movePacket);
        }
    }
}
