package com.zephyr.client.mixin;

import com.zephyr.client.ZephyrClient;
import com.zephyr.client.module.EntityControl;
import com.zephyr.client.module.ItemRestock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private static final byte TOTEM_POP_STATUS = 35;

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void zephyr$trackTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || packet.getStatus() != TOTEM_POP_STATUS) {
            return;
        }

        if (packet.getEntity(mc.world) == mc.player) {
            ItemRestock.onTotemPop(mc);
        }
    }

    @Inject(method = "onVehicleMove", at = @At("HEAD"), cancellable = true)
    private void onVehicleMove(VehicleMoveS2CPacket packet, CallbackInfo ci) {

        if (EntityControl.onReceivePacket(packet)) {
            ci.cancel();
        }
    }

}
