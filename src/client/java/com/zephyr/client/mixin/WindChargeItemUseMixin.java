package com.zephyr.client.mixin;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.zephyr.client.module.PearlCatch;

//i lied this is an ENDER PEARL mixin
@Mixin(EnderPearlItem.class)
public class WindChargeItemUseMixin {

    private static boolean queued = false;
    private static int originalSlot = -1;

    private static int delayTicksRemaining = 0;

    private static int savedPitch = 0;
    private static int savedYaw = 0;

    private static void storePitchYaw(PlayerEntity player) {
        savedPitch = (int) player.getPitch();
        savedYaw = (int) player.getYaw();
    }


    private static void setLookStraightUp(PlayerEntity player) {
        player.setPitch(-90.0f);
    }

    private static void restorePitchYaw(PlayerEntity player) {
        player.setPitch((float) savedPitch);
        player.setYaw((float) savedYaw);
    }

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!queued) return;

            ClientPlayerEntity player = client.player;
            ClientPlayerInteractionManager im = client.interactionManager;

            if (player == null || im == null) {
                queued = false;
                delayTicksRemaining = 0;
                return;
            }

            if (delayTicksRemaining > 0) {
                delayTicksRemaining--;
                return;
            }

            PlayerInventory inv = player.getInventory();

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);

                if (!stack.isEmpty() && stack.isOf(Items.WIND_CHARGE)) {

                    inv.setSelectedSlot(i);

                    HitResult hit = client.crosshairTarget;

                    if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                        im.interactBlock(player, Hand.MAIN_HAND, (BlockHitResult) hit);
                    } else {
                        im.interactItem(player, Hand.MAIN_HAND);
                    }

                    player.swingHand(Hand.MAIN_HAND);
                    break;
                }
            }

            if (originalSlot != -1) {
                inv.setSelectedSlot(originalSlot);
                restorePitchYaw(player);
                originalSlot = -1;
            }

            queued = false;
        });
    }

    @Inject(method = "use", at = @At("HEAD"))
    private void onWindChargeUse(World world, PlayerEntity user, Hand hand,
                                 CallbackInfoReturnable<ActionResult> cir) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!PearlCatch.enabled) return;

        storePitchYaw(user);
        setLookStraightUp(user);

        PlayerInventory inv = client.player.getInventory();

        originalSlot = inv.getSelectedSlot();

        delayTicksRemaining = PearlCatch.delayTicks;

        queued = true;
    }
}