package com.zephyr.client.mixin.combat.LungeSwap;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.module.combat.LungeSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for {@link Minecraft}. Injects into the HEAD of {@code Minecraft#startAttack} to back
 * the {@code LungeSwap} module: when the player attacks with no valid hit result (a MISS), the
 * mixin swaps to the highest-Lunge spear in the hotbar and re-triggers the attack via
 * {@link ForceAttackMixin} so the empty swing becomes a lunging spear attack.
 */
@Mixin(Minecraft.class)
public class AttackMixin {
    private static int previousSlot = -1;
    private static boolean isProcessingAttack = false;

    /** Returns true if the given stack is any vanilla spear variant. */
    private static boolean isSpear(ItemStack stack) {
        return stack.is(Items.WOODEN_SPEAR)
                || stack.is(Items.STONE_SPEAR)
                || stack.is(Items.COPPER_SPEAR)
                || stack.is(Items.IRON_SPEAR)
                || stack.is(Items.GOLDEN_SPEAR)
                || stack.is(Items.DIAMOND_SPEAR)
                || stack.is(Items.NETHERITE_SPEAR);
    }


    /** Returns the hotbar slot of the spear with the highest Lunge enchantment level, or -1 if none exists. */
    private static int findBestSlot(Minecraft client) {
        int bestSlot = -1;
        int bestLevel = -1;

        var lunge = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LUNGE);

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);

            if (!isSpear(stack))
                continue;

            int level = stack.getEnchantments().getLevel(lunge);

            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    /** Performs the Lunge swap and re-attack at the HEAD of a MISSING startAttack when enabled. */
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft)(Object) this;
        if (client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS) {
            if (!LungeSwap.INSTANCE.isEnabled()) return;
            if (isProcessingAttack) return;

            int best = findBestSlot(client);
            if (best != -1) {
                int originalSlot = client.player.getInventory().getSelectedSlot();
                previousSlot = originalSlot;
                client.player.getInventory().setSelectedSlot(best);

                isProcessingAttack = true;
                try {
                    ((ForceAttackMixin) client).invokeDoAttack();
                } finally {
                    isProcessingAttack = false;
                }

                // Delay should affect swapping BACK, not swapping to the spear
                if (LungeSwap.INSTANCE.legit.get()) {
                    int swapDelay = LungeSwap.INSTANCE.delay.get().intValue();
                    // placementDelay is alias to delay (InstaCart/XBowCart compatibility)
                    swapDelay = LungeSwap.INSTANCE.placementDelay.get().intValue();
                    if (swapDelay <= 0) {
                        client.player.getInventory().setSelectedSlot(previousSlot);
                        previousSlot = -1;
                    } else {
                        int restoreSlot = previousSlot;
                        previousSlot = -1;
                        TickScheduler.schedule(swapDelay, () -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null) {
                                mc.player.getInventory().setSelectedSlot(restoreSlot);
                            }
                        });
                    }
                } else {
                    client.player.getInventory().setSelectedSlot(previousSlot);
                    previousSlot = -1;
                }
            }
        }
    }
}
