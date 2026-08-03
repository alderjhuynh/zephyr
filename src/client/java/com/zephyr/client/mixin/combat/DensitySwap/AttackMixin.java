package com.zephyr.client.mixin.combat.DensitySwap;

import com.zephyr.client.mixin.combat.BreachSwap.ForceAttackMixin;
import com.zephyr.client.module.combat.BreachSwap;
import com.zephyr.client.module.combat.DensitySwap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {
    private static int previousSlot = -1;
    private static boolean isProcessingAttack = false;


    private static int findBestSlot(Minecraft client) {
        int bestSlot = -1;
        int bestLevel = -1;

        var density = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.DENSITY);

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);

            if (!stack.is(Items.MACE))
                continue;

            int level = stack.getEnchantments().getLevel(density);

            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onStartAttack(Player player, Entity entity, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
            if (client.player.fallDistance >= DensitySwap.INSTANCE.minFall.get()) {
                if (BreachSwap.INSTANCE.isEnabled()) {
                    if (isProcessingAttack) return;

                    int best = findBestSlot(client);
                    if (best != -1) {
                        previousSlot = client.player.getInventory().getSelectedSlot();
                        client.player.getInventory().setSelectedSlot(best);

                        isProcessingAttack = true;
                        try {
                            ((ForceAttackMixin) client).invokeDoAttack();
                        } finally {
                            isProcessingAttack = false;
                        }
                        client.player.getInventory().setSelectedSlot(previousSlot);
                        previousSlot = -1;
                    }
                }
            }
        }
}
