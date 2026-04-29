package com.zephyr.client.mixin;

import com.zephyr.client.module.EntityControl;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class EntityControlConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$applyModulePacketHooks(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof VehicleMoveC2SPacket) {
            EntityControl.onSendPacket(packet);

            if (EntityControl.enabled && EntityControl.antiKick) {
                ci.cancel();
            }
        }
    }
}
