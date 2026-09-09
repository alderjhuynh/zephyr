package com.zephyr.client.mixin.combat.BreachSwap;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.module.combat.BreachSwap;
import com.zephyr.client.module.combat.MaceSwapGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Mixin for {@link MultiPlayerGameMode}. Injects into the HEAD of {@code MultiPlayerGameMode#attack}
 * to back the {@code BreachSwap} module: when the local player's fall distance is at or below
 * the configured maximum, the mixin swaps to the highest-Breach mace in the hotbar and
 * re-triggers the attack via {@link ForceAttackMixin}, so the swing lands with the Breach
 * enchantment active.
 */
@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("zephyr/BreachSwap");
    private static final boolean DIAGNOSTICS = false;
    private static final Random RANDOM = new Random();


    /** Returns the hotbar slot of the mace with the highest Breach enchantment level, or -1 if none exists. */
    private static int findBestBreachSlot(Minecraft client) {
        if (DIAGNOSTICS) LOGGER.info("[BreachSwap] ENTER findBestBreachSlot class={} thread={}", AttackMixin.class.getName(), Thread.currentThread().getName());
        int bestSlot = -1;
        int bestLevel = 0;

        var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> breach = registry.getOrThrow(Enchantments.BREACH);
        Holder<Enchantment> densityHolder = null;
        try {
            densityHolder = registry.getOrThrow(Enchantments.DENSITY);
        } catch (Exception e) {
            LOGGER.warn("[BreachSwap] Failed to resolve DENSITY holder for diagnostics", e);
        }

        if (DIAGNOSTICS) {
            LOGGER.info("[BreachSwap] findBestSlot scan: fallDistance={} maxFall={} isEnabled={}",
                    client.player.fallDistance, BreachSwap.INSTANCE.maxFall.get(), BreachSwap.INSTANCE.isEnabled());
        }

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            boolean isMace = stack.is(Items.MACE);
            int breachLevel = isMace ? stack.getEnchantments().getLevel(breach) : -1;
            int densityLevel = -1;
            if (isMace && densityHolder != null) {
                densityLevel = stack.getEnchantments().getLevel(densityHolder);
            }
            if (DIAGNOSTICS) {
                String itemDesc = isMace ? "MACE" : (stack.isEmpty() ? "EMPTY" : stack.getItem().toString());
                LOGGER.info("[BreachSwap]  slot {}: item={} breachLv={} densityLv={} count={} ench={}",
                        i, itemDesc, breachLevel, densityLevel, stack.getCount(), stack.getEnchantments().entrySet());
            }
            if (!isMace)
                continue;

            if (breachLevel <= 0) continue;

            if (breachLevel > bestLevel) {
                bestLevel = breachLevel;
                bestSlot = i;
            }
        }

        if (DIAGNOSTICS) {
            LOGGER.info("[BreachSwap] findBestSlot result: bestSlot={} bestBreachLevel={}", bestSlot, bestLevel);
        }

        return bestSlot;
    }

    /** Performs the Breach swap and re-attack at the HEAD of each attack when conditions are met. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(Player player, Entity entity, CallbackInfo ci) {
        if (MaceSwapGuard.isProcessingAttack) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: guarded re-entry, skipping");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: client null, skipping");
            return;
        }
        if (!BreachSwap.INSTANCE.isEnabled()) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: module disabled, skipping");
            return;
        }
        double maxFall = BreachSwap.INSTANCE.maxFall.get();
        double fall = client.player.fallDistance;
        if (!(fall <= maxFall)) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: fallDistance {} > maxFall {}, skipping", fall, maxFall);
            return;
        }
        if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: conditions met (fall={} <= maxFall={}), scanning", fall, maxFall);
        int best = findBestBreachSlot(client);
        if (DIAGNOSTICS) LOGGER.info("[BreachSwap] findBestBreachSlot returned best={}", best);
        if (best == -1) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: no Breach mace found, not swapping");
            return;
        }
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (best == currentSlot) {
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: bestSlot already selected, no swap needed");
            return;
        }
        if (DIAGNOSTICS) LOGGER.info("[BreachSwap] onStartAttack: swapping {} -> {} (Breach)", currentSlot, best);
        MaceSwapGuard.previousSlot = currentSlot;
        client.player.getInventory().setSelectedSlot(best);
        MaceSwapGuard.isProcessingAttack = true;
        boolean attacked = false;
        try {
            attacked = ((ForceAttackMixin) client).invokeDoAttack();
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] invokeDoAttack returned {}", attacked);
            if (attacked) {
                ci.cancel();
                if (DIAGNOSTICS) LOGGER.info("[BreachSwap] Cancelled original attack");
            } else {
                if (DIAGNOSTICS) LOGGER.warn("[BreachSwap] invokeDoAttack false - not cancelling");
            }
        } finally {
            MaceSwapGuard.isProcessingAttack = false;
        }
        // Delay should affect swapping BACK, not swapping to the mace.
        // Schedule restore so original attack (when attacked==false) still sees the mace,
        // and swapped attack (when attacked==true) shows visual swap for the delay duration.
        if (BreachSwap.INSTANCE.legit.get()) {
            int swapDelay = BreachSwap.INSTANCE.delay.get().intValue();
            swapDelay = BreachSwap.INSTANCE.placementDelay.get().intValue();
            int jitter = BreachSwap.INSTANCE.jitter.get().intValue();
            int effectiveDelay = swapDelay + (jitter > 0 ? RANDOM.nextInt(jitter + 1) : 0);
            if (DIAGNOSTICS) LOGGER.info("[BreachSwap] Legit enabled: baseDelay={} jitter={} effectiveDelay={} attacked={}", swapDelay, jitter, effectiveDelay, attacked);
            if (effectiveDelay <= 0) {
                if (!attacked) {
                    int restoreSlot = MaceSwapGuard.previousSlot;
                    MaceSwapGuard.previousSlot = -1;
                    TickScheduler.schedule(1, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) mc.player.getInventory().setSelectedSlot(restoreSlot);
                    });
                } else {
                    client.player.getInventory().setSelectedSlot(MaceSwapGuard.previousSlot);
                    if (DIAGNOSTICS) LOGGER.info("[BreachSwap] Restored slot -> {}", MaceSwapGuard.previousSlot);
                    MaceSwapGuard.previousSlot = -1;
                }
            } else {
                int restoreSlot = MaceSwapGuard.previousSlot;
                MaceSwapGuard.previousSlot = -1;
                TickScheduler.schedule(effectiveDelay, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.getInventory().setSelectedSlot(restoreSlot);
                        if (DIAGNOSTICS) LOGGER.info("[BreachSwap] Scheduled restore -> {}", restoreSlot);
                    }
                });
            }
        } else {
            if (!attacked) {
                int restoreSlot = MaceSwapGuard.previousSlot;
                MaceSwapGuard.previousSlot = -1;
                TickScheduler.schedule(1, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) mc.player.getInventory().setSelectedSlot(restoreSlot);
                });
            } else {
                client.player.getInventory().setSelectedSlot(MaceSwapGuard.previousSlot);
                if (DIAGNOSTICS) LOGGER.info("[BreachSwap] Restored slot -> {}", MaceSwapGuard.previousSlot);
                MaceSwapGuard.previousSlot = -1;
            }
        }
    }
}
