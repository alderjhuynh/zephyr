package com.zephyr.client.mixin.movement.Blink;

import com.zephyr.client.module.movement.Blink;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's Blink (which cancels LocalPlayer.sendPosition).
// 1.21.1 has no sendPosition; position packets are sent straight through
// Connection.send, so cancel ServerboundMovePlayerPacket there instead.
// On disable the next tick sends the current position, same as 26.3.
@Mixin(Connection.class)
public abstract class ClientConnectionMixin {

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$blink(Packet<?> packet, PacketSendListener packetSendListener, boolean flush, CallbackInfo ci) {
        if (Blink.INSTANCE.isEnabled() && packet instanceof ServerboundMovePlayerPacket) {
            ci.cancel();
        }
    }
}
