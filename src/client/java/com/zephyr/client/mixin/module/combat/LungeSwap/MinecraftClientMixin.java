package com.zephyr.client.mixin.module.combat.LungeSwap;

import com.zephyr.client.module.combat.MaceSwap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private static int previousSlot = -1;
    private static boolean swapped = false;
    private static int lastAttackTick = -1;

    private static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        for (RegistryEntry<Enchantment> ench : stack.getEnchantments().getEnchantments()) {
            if (ench.toString().contains(enchantment.getValue().toString())) {
                return stack.getEnchantments().getLevel(ench);
            }
        }
        return 0;
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onAttackStart(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        if (!MaceSwap.enabled) return;

        int tick = client.player.age;
        if (tick == lastAttackTick) return;
        lastAttackTick = tick;

        if (swapped) return;

        PlayerInventory inv = client.player.getInventory();

        int bestSlot = -1;
        int bestLevel = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);

            if (stack.isOf(Items.NETHERITE_SPEAR)) {
                int level = getLevel(stack, Enchantments.LUNGE);

                if (level > bestLevel) {
                    bestLevel = level;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            previousSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(bestSlot);
            swapped = true;
        }
    }

    @Inject(method = "doAttack", at = @At("TAIL"))
    private void onAttackEnd(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        if (!MaceSwap.enabled) return;

        if (!swapped) return;

        PlayerInventory inv = client.player.getInventory();

        if (previousSlot != -1) {
            inv.setSelectedSlot(previousSlot);
        }

        swapped = false;
        previousSlot = -1;
    }
}