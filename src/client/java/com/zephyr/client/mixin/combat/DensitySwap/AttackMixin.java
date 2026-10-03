package com.zephyr.client.mixin.combat.DensitySwap;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.mixin.combat.BreachSwap.ForceAttackMixin;
import com.zephyr.client.module.combat.DensitySwap;
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

@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("zephyr/DensitySwap");
    private static final boolean DIAGNOSTICS = false;
    private static final Random RANDOM = new Random();

    private static int findBestDensitySlot(Minecraft client) {
        if (DIAGNOSTICS) LOGGER.info("[DensitySwap] ENTER findBestDensitySlot class={} thread={}", AttackMixin.class.getName(), Thread.currentThread().getName());
        int bestSlot = -1;
        int bestLevel = 0;

        var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> density = registry.getOrThrow(Enchantments.DENSITY);
        Holder<Enchantment> breachHolder = null;
        try {
            breachHolder = registry.getOrThrow(Enchantments.BREACH);
        } catch (Exception e) {
            LOGGER.warn("[DensitySwap] Failed to resolve BREACH holder for diagnostics", e);
        }

        if (DIAGNOSTICS) {
            LOGGER.info("[DensitySwap] findBestSlot scan: fallDistance={} minFall={} isEnabled={}",
                    client.player.fallDistance, DensitySwap.INSTANCE.minFall.get(), DensitySwap.INSTANCE.isEnabled());
        }

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            boolean isMace = stack.is(Items.MACE);
            int densityLevel = isMace ? stack.getEnchantments().getLevel(density) : -1;
            int breachLevel = -1;
            if (isMace && breachHolder != null) {
                breachLevel = stack.getEnchantments().getLevel(breachHolder);
            }

            if (DIAGNOSTICS) {
                String itemDesc = isMace ? "MACE" : (stack.isEmpty() ? "EMPTY" : stack.getItem().toString());
                LOGGER.info("[DensitySwap]  slot {}: item={} densityLv={} breachLv={} count={} ench={}",
                        i, itemDesc, densityLevel, breachLevel, stack.getCount(), stack.getEnchantments().entrySet());
            }

            if (!isMace)
                continue;

            if (densityLevel <= 0) continue;

            if (densityLevel > bestLevel) {
                bestLevel = densityLevel;
                bestSlot = i;
            }
        }

        if (DIAGNOSTICS) {
            LOGGER.info("[DensitySwap] findBestSlot result: bestSlot={} bestDensityLevel={}", bestSlot, bestLevel);
            if (bestSlot == -1) {
                LOGGER.info("[DensitySwap] No Density mace found - attack will NOT be swapped (this explains 'doesn't work without Breach' if the only mace has Density 0)");
            }
        }

        return bestSlot;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(Player player, Entity entity, CallbackInfo ci) {
        if (MaceSwapGuard.isProcessingAttack) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: guarded re-entry, skipping");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: client.player or level null, skipping");
            return;
        }
        if (!DensitySwap.INSTANCE.isEnabled()) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: module disabled, skipping");
            return;
        }
        double minFall = DensitySwap.INSTANCE.minFall.get();
        double fall = client.player.fallDistance;
        if (!(fall >= minFall)) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: fallDistance {} < minFall {}, skipping", fall, minFall);
            return;
        }

        if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: conditions met (fall={} >= minFall={}), scanning hotbar", fall, minFall);

        int best = findBestDensitySlot(client);
        if (DIAGNOSTICS) LOGGER.info("[DensitySwap] findBestDensitySlot returned best={}", best);
        if (best == -1) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: no Density mace found, not swapping (original attack will run with slot {})", client.player.getInventory().selected);
            return;
        }

        int currentSlot = client.player.getInventory().selected;
        if (best == currentSlot) {
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: bestSlot {} already selected, no swap needed", best);
            return;
        }

        if (DIAGNOSTICS) LOGGER.info("[DensitySwap] onStartAttack: swapping slot {} -> {} (Density), then re-triggering attack", currentSlot, best);

        MaceSwapGuard.previousSlot = currentSlot;
        client.player.getInventory().selected = best;

        MaceSwapGuard.isProcessingAttack = true;
        boolean attacked = false;
        try {
            attacked = ((ForceAttackMixin) client).invokeDoAttack();
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] invokeDoAttack returned {}", attacked);
            if (attacked) {
                ci.cancel();
                if (DIAGNOSTICS) LOGGER.info("[DensitySwap] Cancelled original attack, swapped attack completed");
            } else {
                if (DIAGNOSTICS) LOGGER.warn("[DensitySwap] invokeDoAttack returned false - NOT cancelling original attack (will restore slot and let original run)");
            }
        } finally {
            MaceSwapGuard.isProcessingAttack = false;
        }
        // Delay should affect swapping BACK, not swapping to the mace.
        // Schedule restore so original attack (when attacked==false) still sees the mace,
        // and swapped attack (when attacked==true) shows visual swap for the delay duration.
        if (DensitySwap.INSTANCE.legit.get()) {
            int swapDelay = DensitySwap.INSTANCE.delay.get().intValue();
            // placementDelay is alias to delay (InstaCart/XBowCart naming compatibility)
            swapDelay = DensitySwap.INSTANCE.placementDelay.get().intValue();
            int jitter = DensitySwap.INSTANCE.jitter.get().intValue();
            int effectiveDelay = swapDelay + (jitter > 0 ? RANDOM.nextInt(jitter + 1) : 0);
            if (DIAGNOSTICS) LOGGER.info("[DensitySwap] Legit enabled: baseDelay={} jitter={} effectiveDelay={} attacked={}", swapDelay, jitter, effectiveDelay, attacked);
            if (effectiveDelay <= 0) {
                // Even with 0 delay, defer by 1 tick when original wasn't cancelled so it can run with mace.
                if (!attacked) {
                    int restoreSlot = MaceSwapGuard.previousSlot;
                    MaceSwapGuard.previousSlot = -1;
                    TickScheduler.schedule(1, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) mc.player.getInventory().selected = restoreSlot;
                    });
                } else {
                    client.player.getInventory().selected = MaceSwapGuard.previousSlot;
                    if (DIAGNOSTICS) LOGGER.info("[DensitySwap] Restored slot -> {}", MaceSwapGuard.previousSlot);
                    MaceSwapGuard.previousSlot = -1;
                }
            } else {
                int restoreSlot = MaceSwapGuard.previousSlot;
                MaceSwapGuard.previousSlot = -1;
                TickScheduler.schedule(effectiveDelay, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.getInventory().selected = restoreSlot;
                        if (DIAGNOSTICS) LOGGER.info("[DensitySwap] Scheduled restore -> {}", restoreSlot);
                    }
                });
            }
        } else {
            if (!attacked) {
                // Keep mace selected for the original attack which runs immediately after this HEAD.
                // Schedule 1-tick restore so original sees mace.
                int restoreSlot = MaceSwapGuard.previousSlot;
                MaceSwapGuard.previousSlot = -1;
                TickScheduler.schedule(1, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) mc.player.getInventory().selected = restoreSlot;
                });
            } else {
                client.player.getInventory().selected = MaceSwapGuard.previousSlot;
                if (DIAGNOSTICS) LOGGER.info("[DensitySwap] Restored slot -> {}", MaceSwapGuard.previousSlot);
                MaceSwapGuard.previousSlot = -1;
            }
        }
    }
}
