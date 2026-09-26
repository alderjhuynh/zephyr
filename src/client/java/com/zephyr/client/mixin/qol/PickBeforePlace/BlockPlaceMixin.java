package com.zephyr.client.mixin.qol.PickBeforePlace;

import com.zephyr.client.module.qol.PickBeforePlace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link MultiPlayerGameMode} implementing the Zephyr PickBeforePlace
 * module.
 *
 * <p>Injects at the head of {@code MultiPlayerGameMode.useItemOn}. When the
 * module is enabled and the main-hand use targets a block, a pick-block action
 * is forced via {@link BlockPickInvoker} before the place proceeds. A re-entry
 * flag prevents the injected pick from triggering this mixin recursively.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class BlockPlaceMixin {

    /** The Minecraft client instance, used to perform the pick-block. */
    @Shadow @Final private Minecraft minecraft;

    /**
     * Guards against recursion: true while the forced pick-block's own
     * {@code useItemOn} is executing.
     */
    @Unique
    private boolean isPicking = false;

    /**
     * Forces a pick-block action before placing a block when the module is
     * enabled, then forwards the original use request and returns its result.
     *
     * @param player   the player using the item
     * @param hand     the hand the item is used with
     * @param hitResult the block hit being targeted
     * @param cir      mixin callback used to substitute the whole use
     */
    @Inject(
            method = "useItemOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (isPicking) return;
        if (!PickBeforePlace.INSTANCE.isEnabled()) return;

        if (hand != InteractionHand.MAIN_HAND) return;

        if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        isPicking = true;

        ((BlockPickInvoker) minecraft).invokePickBlock();

        InteractionResult result = ((MultiPlayerGameMode)(Object)this)
                .useItemOn(player, hand, hitResult);

        isPicking = false;

        cir.setReturnValue(result);
    }
}