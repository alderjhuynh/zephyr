package com.zephyr.client.mixin.combat.BreachSwap;

import com.zephyr.client.module.combat.BreachSwap;
import com.zephyr.client.module.combat.MaceSwapGuard;
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

/**
 * Mixin for {@link MultiPlayerGameMode}. Injects into the HEAD of {@code MultiPlayerGameMode#attack}
 * to back the {@code BreachSwap} module: when the local player's fall distance is at or below
 * the configured maximum, the mixin swaps to the highest-Breach mace in the hotbar and
 * re-triggers the attack via {@link ForceAttackMixin}, so the swing lands with the Breach
 * enchantment active.
 */
@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {


    /** Returns the hotbar slot of the mace with the highest Breach enchantment level, or -1 if none exists. */
    private static int findBestSlot(Minecraft client) {
        int bestSlot = -1;
        int bestLevel = 0;

        var breach = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BREACH);

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);

            if (!stack.is(Items.MACE))
                continue;

            int level = stack.getEnchantments().getLevel(breach);
            if (level <= 0) continue;

            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    /** Performs the Breach swap and re-attack at the HEAD of each attack when conditions are met. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(Player player, Entity entity, CallbackInfo ci) {
        if (MaceSwapGuard.isProcessingAttack) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (!BreachSwap.INSTANCE.isEnabled()) return;
        if (!(client.player.fallDistance <= BreachSwap.INSTANCE.maxFall.get())) return;

        int best = findBestSlot(client);
        if (best == -1) return;

        MaceSwapGuard.previousSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(best);

        MaceSwapGuard.isProcessingAttack = true;
        try {
            ((ForceAttackMixin) client).invokeDoAttack();
            ci.cancel();
        } finally {
            MaceSwapGuard.isProcessingAttack = false;
        }
        client.player.getInventory().setSelectedSlot(MaceSwapGuard.previousSlot);
        MaceSwapGuard.previousSlot = -1;
    }
}
