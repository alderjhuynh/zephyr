package com.zephyr.client.mixin.disable.TotemAnimation;

import com.zephyr.client.module.disable.disableTotemAnimation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableTotemAnimation(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!disableTotemAnimation.INSTANCE.isEnabled()) return;
        if (packet.getEventId() == 35) {
            ci.cancel();
        }
    }
}
