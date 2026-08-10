package com.zephyr.client.mixin.qol.ItemRestock;

import com.zephyr.client.module.qol.ItemRestock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link MultiPlayerGameMode} that observes item use actions so the
 * Zephyr ItemRestock module can refill a hand when its item is consumed.
 *
 * <p>Hooks {@code MultiPlayerGameMode.useItem} and {@code useItemOn}. For block
 * uses, the held stack is captured at the head of {@code useItemOn} and both
 * results are passed to {@link ItemRestock#trackUse} at the return so only
 * genuinely consumed uses trigger a restock.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    /**
     * Tracks a successful item-use action that may have consumed the held item.
     *
     * @param player the player performing the use
     * @param hand   the hand the item was used from
     * @param cir    the use result (checked for {@code consumesAction()})
     */
    @Inject(method = "useItem", at = @At("RETURN"))
    private void zephyr$trackItemUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        ItemRestock.trackUse(client, hand, ItemRestock.consumeCapturedUse(hand), cir.getReturnValue());
    }

    /**
     * Captures the item stack held in the used hand at the start of a block-use
     * attempt, so its exact pre-use state can be compared at the return.
     *
     * @param player   the player performing the use
     * @param hand     the hand the item is used with
     * @param hitResult the block hit being targeted
     * @param cir      mixin callback info (unused)
     */
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void zephyr$captureBlockUse(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        ItemRestock.captureUseAttempt(hand, player.getItemInHand(hand));
    }

    /**
     * Tracks a successful block-use action that may have consumed the held item.
     *
     * @param player   the player performing the use
     * @param hand     the hand the item was used with
     * @param hitResult the block hit that was targeted
     * @param cir      the use result (checked for {@code consumesAction()})
     */
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void zephyr$trackBlockUse(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        ItemRestock.trackUse(client, hand, ItemRestock.consumeCapturedUse(hand), cir.getReturnValue());
    }
}