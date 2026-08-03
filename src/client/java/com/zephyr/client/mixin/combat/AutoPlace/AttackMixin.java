package com.zephyr.client.mixin.combat.AutoPlace;

import com.zephyr.client.module.combat.AutoPlace;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {
    private static int previousSlot = -1;
    private static int lavaCleanupTimer = -1;
    private static Entity cleanupTarget;
    private static int cleanupPreviousSlot;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (lavaCleanupTimer < 0)
                return;

            if (--lavaCleanupTimer == 0) {
                lavaCleanup(client);
                lavaCleanupTimer = -1;
            }
        });
    }

    private static int findItemSlot(Minecraft client, Item targetItem) {
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackEnd(Player player, Entity entity, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (AutoPlace.INSTANCE.isEnabled()) {
            previousSlot = client.player.getInventory().getSelectedSlot();
            if (AutoPlace.getMode().equals(AutoPlace.Mode.LAVA)) {
                int itemSlot = findItemSlot(client, Items.LAVA_BUCKET);
                if (itemSlot == -1) return;
                client.player.getInventory().setSelectedSlot(itemSlot);
                simulateBucketUse(client, entity);
            } else if (AutoPlace.getMode().equals(AutoPlace.Mode.WEB)) {
                int itemSlot = findItemSlot(client, Items.COBWEB);
                if (itemSlot == -1) return;
                client.player.getInventory().setSelectedSlot(itemSlot);
                simulateBlockPlace(client, getTargetBlockPos(entity));
            } else return;
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private BlockPos getTargetBlockPos(Entity entity) {
        return entity.blockPosition();
    }

    private static void lavaCleanup(Minecraft client) {
        if (cleanupTarget == null || client.player == null)
            return;

        int bucketSlot = findItemSlot(client, Items.BUCKET);
        if (bucketSlot == -1)
            return;

        int previous = client.player.getInventory().getSelectedSlot();

        client.player.getInventory().setSelectedSlot(bucketSlot);
        simulateBucketUse(client, cleanupTarget);
        client.player.getInventory().setSelectedSlot(previous);

        cleanupTarget = null;
    }

    private static void simulateBucketUse(Minecraft client, Entity entity) {
        if (client.player == null || client.gameMode == null)
            return;

        BlockPos placePos = entity.blockPosition();
        BlockPos support = placePos.below();

        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(support).add(0, 0.5, 0),
                Direction.UP,
                support,
                false
        );

        Vec2 rot = client.player.getRotationVector();
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(placePos));

        client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                hit
        );

        client.player.setXRot(rot.x);
        client.player.setYRot(rot.y);

        cleanupTarget = entity;
        lavaCleanupTimer = 1;
    }

    private void simulateBlockPlace(Minecraft client, BlockPos pos) {
        BlockPos support = pos.below();
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(support).add(0, 0.5, 0),
                Direction.UP,
                support,
                false
        );

        var result = client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                hit
        );
    }
}
