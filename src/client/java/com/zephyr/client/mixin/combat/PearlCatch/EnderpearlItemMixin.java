package com.zephyr.client.mixin.combat.PearlCatch;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.module.combat.PearlCatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Backport of 26.3's EnderpearlItem mixin. 1.21.1 differences: the item class
// is EnderpearlItem (not EnderpearlItem), use() returns
// InteractionResultHolder<ItemStack>, swing() is single-arg, inventory
// selected slot is a field, and the unused LungeSwap import is dropped
// (LungeSwap does not exist in 1.21.1).
@Mixin(EnderpearlItem.class)
public class EnderpearlItemMixin {
    private static int previousSlot = -1;

    private static int findItemSlot(Minecraft client, Item targetItem) {
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    private static void simulateUse(Minecraft client) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

        if (player == null || gameMode == null) return;

        HitResult hit = client.hitResult;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            gameMode.useItemOn(player, InteractionHand.MAIN_HAND, (BlockHitResult) hit);
        } else {
            gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }

        player.swing(InteractionHand.MAIN_HAND);
    }

    @Inject(method = "use", at = @At("TAIL"))
    public void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!PearlCatch.INSTANCE.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        int wcSlot = findItemSlot(client, Items.WIND_CHARGE);

        if (wcSlot != -1) {
            int originalSlot = client.player.getInventory().selected;
            previousSlot = originalSlot;

            if (PearlCatch.INSTANCE.legit.get()) {
                int swapDelay = PearlCatch.INSTANCE.delay.get().intValue();
                if (swapDelay <= 0) {
                    client.player.getInventory().selected = wcSlot;
                    simulateUse(client);
                    client.player.getInventory().selected = previousSlot;
                    previousSlot = -1;
                } else {
                    TickScheduler.schedule(swapDelay, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player == null) {
                            previousSlot = -1;
                            return;
                        }
                        mc.player.getInventory().selected = wcSlot;
                        simulateUse(mc);
                        mc.player.getInventory().selected = originalSlot;
                        previousSlot = -1;
                    });
                }
            } else {
                client.player.getInventory().selected = wcSlot;
                simulateUse(client);
                client.player.getInventory().selected = previousSlot;
                previousSlot = -1;
            }
        }
    }
}
