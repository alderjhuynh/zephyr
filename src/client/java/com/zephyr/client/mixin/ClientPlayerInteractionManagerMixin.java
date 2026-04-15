package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableDeadMobInteraction;
import com.zephyr.client.module.Criticals;
import com.zephyr.client.module.ElytraBoost;
import com.zephyr.client.module.ItemRestock;
import com.zephyr.client.module.PearlBoost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    private void zephyr$sendCriticalPacketsBeforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
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

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void blockAttackDeadEntities(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!disableDeadMobInteraction.enabled) {
            return;
        }

        if (zephyr$isDeadOrDying(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void blockInteractDeadEntities(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!disableDeadMobInteraction.enabled) {
            return;
        }

        if (zephyr$isDeadOrDying(entity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void blockInteractDeadEntitiesAtLocation(
            PlayerEntity player,
            Entity entity,
            net.minecraft.util.hit.EntityHitResult hitResult,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!disableDeadMobInteraction.enabled) {
            return;
        }

        if (zephyr$isDeadOrDying(entity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    private static boolean zephyr$isDeadOrDying(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        return !living.isAlive()
                || living.isRemoved()
                || living.deathTime > 0;
    }
}
