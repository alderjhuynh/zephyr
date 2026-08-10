package com.zephyr.client.mixin.movement.AntiHunger;

import com.zephyr.client.module.movement.AntiHunger;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link net.minecraft.network.Connection}. Injects at the head
 * of the {@code send(Packet, ChannelFutureListener, boolean)} overload, allowing
 * cancellation, and routes each outgoing packet through
 * {@link AntiHunger#onSendPacket}. When that returns {@code false} the packet
 * (e.g. a start-sprint command) is dropped entirely, backing the Anti Hunger
 * module.
 */
@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    /** Cancels packets the Anti Hunger module decides should not be sent. */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$applyAntiHunger(Packet<?> packet, ChannelFutureListener callbacks, boolean flush,
                                        CallbackInfo ci) {
        if (!AntiHunger.onSendPacket(packet, Minecraft.getInstance())) {
            ci.cancel();
        }
    }
}
