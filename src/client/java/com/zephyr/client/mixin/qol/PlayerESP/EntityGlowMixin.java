package com.zephyr.client.mixin.qol.PlayerESP;

import com.zephyr.client.module.qol.PlayerESP;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Entity} implementing the Zephyr PlayerESP module's glow
 * outline.
 *
 * <p>Injects at the head of {@code Entity.isCurrentlyGlowing}. While the module
 * is enabled, every remote player is reported as glowing, causing the vanilla
 * entity-outline pass to draw an outline around them.
 */
@Mixin(Entity.class)
public class EntityGlowMixin {
    /**
     * Makes {@code isCurrentlyGlowing} return {@code true} for remote players
     * while the PlayerESP module is enabled.
     *
     * @param cir mixin callback used to override the glow state
     */
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void glow(CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerESP.INSTANCE.isEnabled()) return;
        if ((Object) this instanceof RemotePlayer) {
            cir.setReturnValue(true);
        }
    }
}
