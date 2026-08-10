package com.zephyr.client.mixin.movement.AntiHunger;

import com.zephyr.client.module.movement.AntiHunger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$applyAntiHunger(Packet<?> packet, PacketSendListener packetSendListener, boolean flush,
                                        CallbackInfo ci) {
        if (!AntiHunger.onSendPacket(packet, Minecraft.getInstance())) {
            ci.cancel();
        }
    }
}
