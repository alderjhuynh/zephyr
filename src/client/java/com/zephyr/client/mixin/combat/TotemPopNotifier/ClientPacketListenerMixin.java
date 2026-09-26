package com.zephyr.client.mixin.combat.TotemPopNotifier;

import com.zephyr.client.module.combat.TotemPopNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ClientPacketListener} backing the {@code TotemPopNotifier} module.
 * Injects at the tail of {@code ClientPacketListener.handleEntityEvent}. When the
 * entity-event packet is the totem-pop status (id 35) for any entity, the entity is handed
 * to {@link TotemPopNotifier#onPop} so other players' totem pops can be reported.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    /** The entity-event status id sent when a totem of undying pops. */
    private static final byte TOTEM_POP_STATUS = 35;

    /**
     * Detects any player's totem pop and forwards it to the module.
     *
     * @param packet the entity-event packet received from the server
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void zephyr$trackTotemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || packet.getEventId() != TOTEM_POP_STATUS) {
            return;
        }

        TotemPopNotifier.onPop(client, packet.getEntity(client.level));
    }
}
