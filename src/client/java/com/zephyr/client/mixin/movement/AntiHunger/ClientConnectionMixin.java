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

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
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
