package com.zephyr.client.mixin;

import com.zephyr.client.Criticals;
import com.zephyr.client.ElytraBoost;
import com.zephyr.client.ItemRestock;
import com.zephyr.client.PearlBoost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(
            method = "attackEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"
            )
    )
    private void zephyr$sendCriticalPacketsBeforeAttack(PlayerEntity player, Entity target, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Criticals.onAttack();
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void zephyr$handleFireworkUse(PlayerEntity player, Hand hand,
                                          CallbackInfoReturnable<ActionResult> cir) {

        if (PearlBoost.isReplayingUse()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ItemStack stack = player.getStackInHand(hand);

        ActionResult pearlBoostResult = PearlBoost.queueThrow(mc, hand);
        if (pearlBoostResult != ActionResult.PASS) {
            cir.setReturnValue(pearlBoostResult);
            return;
        }

        if (!ElytraBoost.isBlockingRockets(stack)) {
            return;
        }

        if (ElytraBoost.canStartBoost(mc, stack)) {
            ElytraBoost.startBoost(mc);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void zephyr$captureItemUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }

        ItemRestock.captureUseAttempt(hand, player.getStackInHand(hand));
    }

    @Inject(method = "interactItem", at = @At("RETURN"))
    private void zephyr$trackItemUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }

        ItemRestock.trackUse(mc, hand, ItemRestock.consumeCapturedUse(hand), cir.getReturnValue());
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void zephyr$captureBlockUse(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }

        ItemRestock.captureUseAttempt(hand, player.getStackInHand(hand));
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void zephyr$trackBlockUse(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                      CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }

        ItemRestock.trackUse(mc, hand, ItemRestock.consumeCapturedUse(hand), cir.getReturnValue());
    }
}
