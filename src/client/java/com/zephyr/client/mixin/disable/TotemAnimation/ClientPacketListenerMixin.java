package com.zephyr.client.mixin.disable.TotemAnimation;

import com.zephyr.client.module.disable.disableTotemAnimation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link ClientPacketListener} that backs the
 * {@code disableTotemAnimation} module.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    /**
     * Intercepts {@code ClientPacketListener#handleEntityEvent} at its head and
     * cancels the entity event with id {@code 35} (the totem of undying pop-up) while
     * the module is enabled, so the totem animation and effects never play.
     *
     * @param packet the entity event packet being handled
     * @param ci     the cancellable injection callback
     */
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableTotemAnimation(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!disableTotemAnimation.INSTANCE.isEnabled()) return;
        if (packet.getEventId() == 35) {
            ci.cancel();
        }
    }
}
