package com.zephyr.client.mixin.qol.SpeedMine;

import com.zephyr.client.module.qol.SpeedMine;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link Connection} that observes every outgoing network packet.
 *
 * <p>Injects at the head of the 3-arg {@code Connection.send} overload and
 * forwards each packet to {@link SpeedMine#onSendPacket}, which uses
 * {@link net.minecraft.network.protocol.game.ServerboundPlayerActionPacket}s to
 * keep its predicted-break state in sync in DAMAGE mode.
 */
@Mixin(Connection.class)
public class ConnectionMixin {
    /**
     * Hands the outgoing packet to the SpeedMine module before it is flushed.
     *
     * @param packet    the packet about to be sent
     * @param callbacks the channel future listener attached to the send
     * @param flush     whether the send should flush the channel
     * @param ci        mixin callback info (unused)
     */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD")
    )
    private void zephyr$applyModulePacketHooks(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        SpeedMine.onSendPacket(packet);
    }
}