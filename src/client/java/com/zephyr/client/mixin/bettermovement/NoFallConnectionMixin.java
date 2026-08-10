package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.NoFall;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class NoFallConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
            at = @At("HEAD")
    )
    private void zephyr$betterMovementNoFall(Packet<?> packet, PacketSendListener packetSendListener, boolean flush, CallbackInfo ci) {
        if (!BetterMovement.enabled) return;
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            NoFall.onSendPacket(movePacket);
        }
    }
}
