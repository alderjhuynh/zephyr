package com.zephyr.client.mixin.qol.ItemRestock;

import com.zephyr.client.module.qol.ItemRestock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ClientPacketListener} that watches for the local player's
 * totem-of-undying pop and hands it to the Zephyr ItemRestock module.
 *
 * <p>Injects at the tail of {@code ClientPacketListener.handleEntityEvent}. When
 * the entity-event packet is the totem-pop status (id 35) for the local player,
 * {@link ItemRestock#onTotemPop} is invoked to open the restock window.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    /** The entity-event status id sent when a totem of undying pops. */
    private static final byte TOTEM_POP_STATUS = 35;

    /**
     * Detects the local player's totem pop and notifies the ItemRestock module.
     *
     * @param packet the entity-event packet received from the server
     * @param ci     mixin callback info (unused)
     */
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