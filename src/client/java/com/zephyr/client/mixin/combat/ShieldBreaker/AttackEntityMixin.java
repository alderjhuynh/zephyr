package com.zephyr.client.mixin.combat.ShieldBreaker;

import com.zephyr.mixin.*;
import com.zephyr.client.module.combat.ShieldBreaker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AttackEntityMixin {

    private static int previousSlot = -1;
    private static boolean isProcessingAttack = false;

    private static int findBestSlot(Minecraft client) {
        int bestSlot = -1;
        int bestLevel = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) bestSlot = i;
        }
        return bestSlot;
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void beforeAttack(Player player, Entity entity, CallbackInfo ci) {
        if (isProcessingAttack) return;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !ShieldBreaker.INSTANCE.isEnabled()) return;

        RemotePlayer targetPlayer = ShieldBreaker.getCrosshairPlayer();

        if (targetPlayer == null) return;

        boolean blocking = ShieldBreaker.isTargetBlockingWithShield(targetPlayer, client.player);

        if (blocking) {
            int best = findBestSlot(client);
            if (best != -1) {
                previousSlot = client.player.getInventory().selected;
                client.player.getInventory().selected = best;

                isProcessingAttack = true;
                try {
                    ((ForceAttackMixin) client).invokeDoAttack();
                } finally {
                    isProcessingAttack = false;
                }

                client.player.getInventory().selected = previousSlot;
                previousSlot = -1;
            }
        };
    }
}
