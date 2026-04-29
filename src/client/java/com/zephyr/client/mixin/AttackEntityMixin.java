package com.zephyr.client.mixin;

import com.zephyr.client.module.ShieldBreaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.AxeItem;

import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Mixin(ClientPlayerInteractionManager.class)
public class AttackEntityMixin {

    private static int previousSlot = -1;
    private static boolean swapped = false;
    private static boolean isReattacking = false; // re-entrancy guard
    private static int swapBackDelay = -1; // will probably add a config value for this
    private static boolean shouldReattack = false;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (swapBackDelay > 0) {
                swapBackDelay--;
            } else if (swapBackDelay == 0) {
                PlayerInventory inv = client.player.getInventory();

                if (previousSlot >= 0 && previousSlot < 9) {
                    inv.setSelectedSlot(previousSlot);
                    shouldReattack = true;
                }

                previousSlot = -1;
                swapped = false;
                swapBackDelay = -1;

                // reattack helper
                if (shouldReattack && client.interactionManager != null) {

                    if (client.crosshairTarget == null) return;

                    isReattacking = true;
                    try {
                        switch (client.crosshairTarget.getType()) {
                            case ENTITY -> {
                                var entityHit = (net.minecraft.util.hit.EntityHitResult) client.crosshairTarget;
                                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            case BLOCK -> {
                                var blockHit = (net.minecraft.util.hit.BlockHitResult) client.crosshairTarget;
                                client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            case MISS -> {
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                        }
                    } finally {
                        isReattacking = false;
                        shouldReattack = false;
                    }
                }
            }
        });
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void beforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (isReattacking) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!ShieldBreaker.enabled) return;
        if (!ShieldBreaker.isCrosshairOnPlayer(client.player)) return;

        PlayerEntity targetPlayer = ShieldBreaker.getCrosshairPlayer();
        if (targetPlayer == null) return;

        boolean blocking = ShieldBreaker.isTargetBlockingWithShield(targetPlayer, client.player);
        PlayerInventory inv = client.player.getInventory();

        if (blocking && !swapped) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                    previousSlot = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    swapped = true;
                    break;
                }
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void afterAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
            if (isReattacking) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.interactionManager == null) return;
            if (!swapped) return;

            PlayerInventory inv = client.player.getInventory();

            if (ShieldBreaker.isLegit) {
                if (previousSlot >= 0 && previousSlot < 9) {
                    swapBackDelay = 1;
                    shouldReattack = false;
                }
            }
            else if (!ShieldBreaker.isLegit) {
                if (client.interactionManager != null) {

                    if (client.crosshairTarget == null) return;

                    isReattacking = true;
                    try {
                        switch (client.crosshairTarget.getType()) {
                            case ENTITY -> {
                                var entityHit = (net.minecraft.util.hit.EntityHitResult) client.crosshairTarget;
                                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            case BLOCK -> {
                                var blockHit = (net.minecraft.util.hit.BlockHitResult) client.crosshairTarget;
                                client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            case MISS -> {
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                        }
                    } finally {
                        isReattacking = false;
                        shouldReattack = false;
                    }
                }
            }
        }
    }