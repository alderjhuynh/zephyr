package com.zephyr.client.mixin.module.combat.MaceSwap;

import com.zephyr.client.module.combat.ShieldBreaker;
import com.zephyr.client.module.combat.MaceSwap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.AxeItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Mixin(ClientPlayerInteractionManager.class)
public class MaceSwapMixin {

    private static int previousSlot = -1;
    private static boolean swapped = false;
    private static boolean isReattacking = false;
    private static int swapBackDelay = -1;
    private static boolean shouldReattack = false;

    // Combined mode state — tracks if we did axe→mace combo
    private static boolean pendingMaceFollowUp = false;

    private static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        for (RegistryEntry<Enchantment> enchantments : stack.getEnchantments().getEnchantments()) {
            if (enchantments.toString().contains(enchantment.getValue().toString())) {
                return stack.getEnchantments().getLevel(enchantments);
            }
        }
        return 0;
    }

    private static int findBestMaceSlot(PlayerInventory inv, float fallDistance) {
        if (fallDistance <= 8) {
            // Ground attack: prefer breach mace
            int bestBreachSlot = -1;
            int bestBreachLevel = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack candidate = inv.getStack(i);
                if (!candidate.isEmpty() && candidate.isOf(Items.MACE)) {
                    int level = getLevel(candidate, Enchantments.BREACH);
                    if (level > bestBreachLevel) {
                        bestBreachLevel = level;
                        bestBreachSlot = i;
                    }
                }
            }

            if (bestBreachSlot != -1) return bestBreachSlot;
        } else {
            // Aerial attack: prefer density, then breach, then unenchanted
            int bestDensitySlot = -1;
            int bestDensityLevel = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack candidate = inv.getStack(i);
                if (!candidate.isEmpty() && candidate.isOf(Items.MACE)) {
                    int level = getLevel(candidate, Enchantments.DENSITY);
                    if (level > bestDensityLevel) {
                        bestDensityLevel = level;
                        bestDensitySlot = i;
                    }
                }
            }

            if (bestDensitySlot != -1) return bestDensitySlot;

            int bestBreachSlot = -1;
            int bestBreachLevel = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack candidate = inv.getStack(i);
                if (!candidate.isEmpty() && candidate.isOf(Items.MACE)) {
                    int level = getLevel(candidate, Enchantments.BREACH);
                    if (level > bestBreachLevel) {
                        bestBreachLevel = level;
                        bestBreachSlot = i;
                    }
                }
            }

            if (bestBreachSlot != -1) return bestBreachSlot;

            for (int i = 0; i < 9; i++) {
                ItemStack candidate = inv.getStack(i);
                if (!candidate.isEmpty() && candidate.isOf(Items.MACE)) {
                    if (candidate.getEnchantments().getEnchantments().isEmpty()) {
                        return i;
                    }
                }
            }
        }

        return -1;
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

        boolean maceEnabled = MaceSwap.enabled;
        boolean shieldEnabled = ShieldBreaker.enabled;

        if (!maceEnabled && !shieldEnabled) return;
        if (!ShieldBreaker.isCrosshairOnPlayer(client.player)) return;

        PlayerEntity targetPlayer = ShieldBreaker.getCrosshairPlayer();
        if (targetPlayer == null) return;

        boolean blocking = ShieldBreaker.isTargetBlockingWithShield(targetPlayer, client.player);
        PlayerInventory inv = client.player.getInventory();

        if (maceEnabled && shieldEnabled && blocking && !swapped) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                    previousSlot = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    swapped = true;
                    pendingMaceFollowUp = true;
                    break;
                }
            }
        } else if (shieldEnabled && blocking && !swapped) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                    previousSlot = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    swapped = true;
                    break;
                }
            }
        } else if (maceEnabled && !blocking && !swapped) {
            Float fallDist = (float) player.fallDistance;
            int maceSlot = findBestMaceSlot(inv, fallDist);
            if (maceSlot != -1) {
                previousSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(maceSlot);
                swapped = true;
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void afterAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (isReattacking) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) return;
        if (!swapped) return;

        if (pendingMaceFollowUp) {
            pendingMaceFollowUp = false;

            PlayerInventory inv = client.player.getInventory();
            Float fallDist = (float) player.fallDistance;
            int maceSlot = findBestMaceSlot(inv, fallDist);

            if (maceSlot != -1) {
                inv.setSelectedSlot(maceSlot);

                if (client.crosshairTarget == null) {
                    swapBackDelay = 1;
                    shouldReattack = false;
                    return;
                }

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
                }
            }

            swapBackDelay = 1;
            shouldReattack = false;

        } else if (ShieldBreaker.isLegit && swapped) {
            if (previousSlot >= 0 && previousSlot < 9) {
                swapBackDelay = 1;
                shouldReattack = false;
            }
        } else if (!ShieldBreaker.isLegit && swapped) {
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
        } else {
            if (previousSlot >= 0 && previousSlot < 9) {
                swapBackDelay = 1;
                shouldReattack = false;
            }
        }
    }
}