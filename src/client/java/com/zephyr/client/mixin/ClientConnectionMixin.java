package com.zephyr.client.mixin;

import com.zephyr.client.module.*;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$applyModulePacketHooks(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!AntiHunger.onSendPacket(packet, mc)) {
            ci.cancel();
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket movePacket) {
            NoFall.onSendPacket(movePacket);
        }

        SpeedMine.onSendPacket(packet);

        if (Blink.onSendPacket(packet)) {
            ci.cancel();
        }
    }
}
