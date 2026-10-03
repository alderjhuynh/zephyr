package com.zephyr.client.mixin.combat.AnchorHelper;

import com.zephyr.client.module.combat.AnchorHelper;
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
 * {@code MultiPlayerGameMode#useItemOn} at the return and triggers the {@code AnchorHelper}
 * module's legit mode when a respawn anchor is placed.
 * The anchor placement is not cancelled; the remaining charge, shield, and detonate steps
 * are scheduled across ticks.
 */
@Mixin(net.minecraft.client.multiplayer.MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void zephyr$anchorHelperAnchorPlace(LocalPlayer player, InteractionHand hand,
                                         BlockHitResult hit,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        AnchorHelper.INSTANCE.onAnchorPlace(player, hand, hit);
    }
}
