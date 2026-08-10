package com.zephyr.client.mixin.qol.SafeWalk;

import com.zephyr.client.module.qol.SafeWalk;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Player} implementing the Zephyr SafeWalk module.
 *
 * <p>Injects at the head of {@code Player.isStayingOnGroundSurface}. While the
 * module is enabled the result is forced to {@code true}, so the player is
 * treated as if sneaking on block edges and cannot walk off them.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /**
     * Makes the player always report that it is staying on the ground surface
     * while the SafeWalk module is enabled.
     *
     * @param cir mixin callback used to force the result to {@code true}
     */
    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void zephyr$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.INSTANCE.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
