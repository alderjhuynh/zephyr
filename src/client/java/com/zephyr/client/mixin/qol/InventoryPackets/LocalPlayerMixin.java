package com.zephyr.client.mixin.qol.InventoryPackets;

import com.mojang.authlib.GameProfile;
import com.zephyr.client.module.qol.InventoryPackets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Redirect(
        method = "closeContainer()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void zephyr$suppressClosePacket(ClientPacketListener listener, Packet<?> packet) {
        if (InventoryPackets.INSTANCE.isEnabled() && this.containerMenu == this.inventoryMenu) {
            return;
        }
        listener.send(packet);
    }
}
