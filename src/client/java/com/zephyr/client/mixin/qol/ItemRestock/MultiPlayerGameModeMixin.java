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

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("RETURN"))
    private void zephyr$trackItemUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        ItemRestock.trackUse(client, hand, ItemRestock.consumeCapturedUse(hand), cir.getReturnValue());
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void zephyr$captureBlockUse(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        ItemRestock.captureUseAttempt(hand, player.getItemInHand(hand));
    }

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