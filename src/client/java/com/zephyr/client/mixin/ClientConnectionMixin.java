package com.zephyr.client.mixin;

import com.zephyr.client.AntiHunger;
import com.zephyr.client.Blink;
import com.zephyr.client.NoFall;
import com.zephyr.client.SpeedMine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$applyModulePacketHooks(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
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
