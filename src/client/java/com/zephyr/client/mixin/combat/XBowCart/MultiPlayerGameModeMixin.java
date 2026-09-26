package com.zephyr.client.mixin.combat.XBowCart;

import com.zephyr.client.module.combat.XBowCart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Mixin for {@link net.minecraft.client.multiplayer.MultiPlayerGameMode}. Injects into
 * {@code MultiPlayerGameMode#useItemOn} at the return and triggers the {@code XBowCart}
 * module's legit mode when a rail is placed while a loaded crossbow is held.
 * The rail placement is not cancelled; the remaining cart, fire, and crossbow steps
 * are scheduled across ticks.
 */
@Mixin(net.minecraft.client.multiplayer.MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void zephyr$xbwCartRailPlace(LocalPlayer player, InteractionHand hand,
                                         BlockHitResult hit,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        XBowCart.INSTANCE.onRailPlace(player, hand, hit);
    }
}
