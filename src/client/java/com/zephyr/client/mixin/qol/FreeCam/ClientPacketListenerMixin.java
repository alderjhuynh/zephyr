package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // Disables freecam when the player respawns/switches dimensions.
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void zephyr$disableOnRespawn(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled()) {
            FreeCam.INSTANCE.setEnabled(false);
        }
    }
}
