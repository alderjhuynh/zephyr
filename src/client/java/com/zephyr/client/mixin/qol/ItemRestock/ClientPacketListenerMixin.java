package com.zephyr.client.mixin.qol.ItemRestock;

import com.zephyr.client.module.qol.ItemRestock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    private static final byte TOTEM_POP_STATUS = 35;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void zephyr$trackTotemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || packet.getEventId() != TOTEM_POP_STATUS) {
            return;
        }

        if (packet.getEntity(client.level) == client.player) {
            ItemRestock.onTotemPop(client);
        }
    }

}