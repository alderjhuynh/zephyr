package com.zephyr.client.mixin.movement.NoFall;

import com.zephyr.client.module.movement.NoFall;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link net.minecraft.network.Connection}. Injects at the head
 * of the {@code send(Packet, ChannelFutureListener, boolean)} overload and feeds
 * every outgoing {@link ServerboundMovePlayerPacket} through
 * {@link NoFall#onSendPacket}, which rewrites the packet's on-ground flag to
 * true while the No Fall module is enabled. Backs the No Fall module.
 */
@Mixin(Connection.class)
public class ClientConnectionMixin {
    /** Rewrites outgoing move packets to mark the player as on ground. */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$applyModulePacketHooks(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            NoFall.onSendPacket(movePacket);
        }
    }
}
