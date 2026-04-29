package com.zephyr.client.mixin;

import com.zephyr.client.module.ShieldBreaker;
import com.zephyr.client.module.BreachSwap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Mixin(ClientPlayerInteractionManager.class)
public class BreachSwapMixin {

    private static int previousSlot = -1;
    private static boolean swapped = false;
    private static boolean isReattacking = false; // re-entrancy guard
    private static int swapBackDelay = -1; // will probably add a config value for this
    private static boolean shouldReattack = false;

    //HOLY FUCK THANK YOU RANDOM REDDIT USER I LOVE YOU (massive shoutout Great_Ad9570)
    private static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantment){
        for (RegistryEntry<Enchantment> enchantments : stack.getEnchantments().getEnchantments()){
            if (enchantments.toString().contains(enchantment.getValue().toString())){
                return stack.getEnchantments().getLevel(enchantments);
            }
        }
        return 0;
    }

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
        if (!BreachSwap.enabled) return;
        if (!ShieldBreaker.isCrosshairOnPlayer(client.player)) return;

        PlayerEntity targetPlayer = ShieldBreaker.getCrosshairPlayer();
        if (targetPlayer == null) return;

        boolean blocking = ShieldBreaker.isTargetBlockingWithShield(targetPlayer, client.player);
        PlayerInventory inv = client.player.getInventory();

        if (!blocking && !swapped) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty()) {

                    if (stack.isOf(Items.MACE)) {

                        int enchantmentLevel = getLevel(stack, Enchantments.BREACH);
                        if (enchantmentLevel > 0) {

                            int bestSlot = -1;
                            int bestLevel = 0;

                            for (int j = 0; j < 9; j++) {
                                ItemStack candidate = inv.getStack(j);

                                if (!candidate.isEmpty()) {

                                    if (candidate.isOf(Items.MACE)) {
                                        int level = getLevel(candidate, Enchantments.BREACH);

                                        if (level > bestLevel) {
                                            bestLevel = level;
                                            bestSlot = j;
                                        }
                                    }
                                }
                            }

                            if (bestSlot != -1) {
                                previousSlot = inv.getSelectedSlot();
                                inv.setSelectedSlot(bestSlot);
                                swapped = true;
                                break;
                            }
                        }
                    }
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

        if (previousSlot >= 0 && previousSlot < 9) {
            swapBackDelay = 1;
            shouldReattack = false;
        }
    }
}