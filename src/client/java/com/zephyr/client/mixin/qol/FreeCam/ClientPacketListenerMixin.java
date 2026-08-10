package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ClientPacketListener} that disables the Zephyr FreeCam
 * module when the player respawns or changes dimensions.
 *
 * <p>Injects at the tail of {@code ClientPacketListener.handleRespawn}, since
 * the detached camera entity is discarded on respawn and would otherwise be
 * left in a broken state.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    /**
     * Turns FreeCam off if it is still enabled after a respawn/dimension change.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void zephyr$disableOnRespawn(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled()) {
            FreeCam.INSTANCE.setEnabled(false);
        }
    }
}
