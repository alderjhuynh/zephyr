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

/**
 * Mixin into {@link LocalPlayer} implementing the Zephyr InventoryPackets
 * module's close-packet suppression.
 *
 * <p>Redirects the packet send inside {@code LocalPlayer.closeContainer}. When
 * the module is enabled and the closed container is the player's own inventory
 * menu, the container-close packet is dropped entirely so items left in the
 * crafting grid persist on the client.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    /**
     * Constructor required by the {@link AbstractClientPlayer} superclass.
     *
     * @param world   the client level
     * @param profile the player's game profile
     */
    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Suppresses the container-close packet when closing the player's own
     * inventory while the InventoryPackets module is enabled, otherwise
     * forwards it to the server as normal.
     *
     * @param listener the packet listener the packet would be sent through
     * @param packet   the container-close packet
     */
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
