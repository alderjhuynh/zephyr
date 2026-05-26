package com.zephyr.client.module.combat;

import com.zephyr.client.mixin.module.combat.LungeSwap.ForceAttackMixin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.HitResult;

public class LungeSwap {

    public static boolean enabled = true;
    public static boolean isLegit = true;

    private static boolean wasKeyDown = false;
    private static int previousSlot = -1;
    private static boolean swapped = false;
    private static int swapBackDelay = -1;
    private static boolean shouldReattack = false;
    private static boolean isReattacking = false;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (swapBackDelay > 0) {
                swapBackDelay--;
            } else if (swapBackDelay == 0) {
                if (previousSlot >= 0 && previousSlot < 9) {
                    client.player.getInventory().setSelectedSlot(previousSlot);
                    shouldReattack = true;
                }

                previousSlot = -1;
                swapped = false;
                swapBackDelay = -1;

                if (shouldReattack && client.interactionManager != null) {
                    isReattacking = true;
                    try {
                        ((ForceAttackMixin) client).invokeDoAttack();
                    } finally {
                        isReattacking = false;
                        shouldReattack = false;
                    }
                }
            }
        });
    }

    private static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        for (RegistryEntry<Enchantment> entry : stack.getEnchantments().getEnchantments()) {
            if (entry.toString().contains(enchantment.getValue().toString())) {
                return stack.getEnchantments().getLevel(entry);
            }
        }
        return 0;
    }

    private static int findBestSpearSlot(MinecraftClient mc) {
        int bestSlot = -1;
        int bestLevel = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.NETHERITE_SPEAR) continue;

            int level = getLevel(stack, Enchantments.LUNGE);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!enabled) return;
        if (mc.interactionManager == null) return;
        if (isReattacking) return;

        boolean keyDown = mc.options.attackKey.isPressed();
        float cooldown = mc.player.getAttackCooldownProgress(0.5f);

        if (keyDown && !wasKeyDown) {
            if (cooldown < 1.0f) {
                wasKeyDown = keyDown;
                return;
            }

            if (mc.crosshairTarget != null &&
                    mc.crosshairTarget.getType() != HitResult.Type.MISS) {
                wasKeyDown = keyDown;
                return;
            }

            int spearSlot = findBestSpearSlot(mc);
            if (spearSlot == -1) {
                wasKeyDown = keyDown;
                return;
            }

            previousSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(spearSlot);
            swapped = true;

            ((ForceAttackMixin) mc).invokeDoAttack();

            // post-attack swap-back, mirroring AttackEntityMixin's afterAttack
            if (swapped) {
                if (isLegit) {
                    swapBackDelay = 1;
                    shouldReattack = false;
                } else {
                    isReattacking = true;
                    try {
                        ((ForceAttackMixin) mc).invokeDoAttack();
                    } finally {
                        isReattacking = false;
                        shouldReattack = false;
                    }

                    if (previousSlot >= 0 && previousSlot < 9) {
                        mc.player.getInventory().setSelectedSlot(previousSlot);
                    }
                    previousSlot = -1;
                    swapped = false;
                }
            }
        }

        wasKeyDown = keyDown;
    }
}