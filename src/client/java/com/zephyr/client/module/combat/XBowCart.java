package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.mixin.combat.AbstractArrowInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class XBowCart extends Module {
    public static final XBowCart INSTANCE = new XBowCart();

    private static final int LOOKAHEAD_TICKS = 8;

    private final Map<Integer, BlockPos> predictions = new HashMap<>();
    private final Set<Integer> processed = new HashSet<>();

    private XBowCart() {
        super("XBowCart", "Places a rail, TNT minecart, and fire to catch your own crossbow arrows", Category.COMBAT);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {
            return;
        }

        if (!hasValidCrossbow(client))
            return;

        LocalPlayer player = client.player;
        double rangeSq = player.blockInteractionRange();
        rangeSq *= rangeSq;

        cleanup(client);

        for (Entity entity : client.level.entitiesForRendering()) {

            if (!(entity instanceof Arrow arrow))
                continue;

            if (arrow.getOwner() != player)
                continue;

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround())
                continue;

            int id = arrow.getId();

            if (processed.contains(id))
                continue;

            if (player.distanceToSqr(arrow) > rangeSq)
                continue;

            predictions.computeIfAbsent(id, ignored -> predict(client, arrow));
        }

        Iterator<Map.Entry<Integer, BlockPos>> iterator = predictions.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, BlockPos> entry = iterator.next();

            Entity entity = client.level.getEntity(entry.getKey());

            if (!(entity instanceof Arrow arrow)) {
                iterator.remove();
                continue;
            }

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround()) {
                iterator.remove();
                processed.add(arrow.getId());
                continue;
            }

            if (place(client, arrow, entry.getValue())) {
                iterator.remove();
                processed.add(arrow.getId());
            }
        }
    }

    private static boolean hasValidCrossbow(Minecraft client) {
        return client.player.getMainHandItem().is(Items.CROSSBOW);
    }

    private static BlockPos predict(Minecraft client, Arrow arrow) {

        Vec3 pos = arrow.position();
        Vec3 velocity = arrow.getDeltaMovement();

        for (int i = 0; i < LOOKAHEAD_TICKS; i++) {

            Vec3 next = pos.add(velocity);

            BlockHitResult hit = arrow.level().clip(new ClipContext(
                    pos,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    arrow
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getBlockPos().relative(hit.getDirection());
            }

            pos = next;
        }

        return null;
    }

    private static boolean place(Minecraft client, Arrow arrow, BlockPos railPos) {

        if (railPos == null)
            return false;

        LocalPlayer player = client.player;

        int fireSlot = findFlintAndSteelSlot(player);
        int railSlot = findRailSlot(player);
        int cartSlot = findMinecartSlot(player);

        if (fireSlot == -1
                || railSlot == -1
                || cartSlot == -1)
            return false;

        BlockPos firePos = findFirePos(client, arrow, railPos);

        if (firePos == null)
            return false;

        int previousSlot = player.getInventory().selected;

        player.getInventory().selected = fireSlot;
        placeFire(client, firePos);

        player.getInventory().selected = railSlot;

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(railPos),
                        Direction.UP,
                        railPos.below(),
                        false
                )
        );

        player.getInventory().selected = cartSlot;

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(railPos),
                        Direction.UP,
                        railPos,
                        false
                )
        );

        player.getInventory().selected = previousSlot;

        return true;
    }

    private static BlockPos findFirePos(Minecraft client, Arrow arrow, BlockPos railPos) {

        Vec3 velocity = arrow.getDeltaMovement();
        Direction back = Direction.getNearest(velocity.x, velocity.y, velocity.z).getOpposite();

        if (back.getAxis().isHorizontal()) {
            BlockPos candidate = railPos.relative(back);
            if (canPlaceFire(client, candidate)) {
                return candidate;
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = railPos.relative(direction);
            if (canPlaceFire(client, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean canPlaceFire(Minecraft client, BlockPos pos) {
        return BaseFireBlock.canBePlacedAt(client.level, pos, Direction.NORTH);
    }

    private static void placeFire(Minecraft client, BlockPos firePos) {

        LocalPlayer player = client.player;

        BlockPos support = firePos.below();

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(support),
                        Direction.UP,
                        support,
                        false
                )
        );
    }

    private static boolean isRail(ItemStack stack) {
        return stack.is(Items.RAIL)
                || stack.is(Items.POWERED_RAIL)
                || stack.is(Items.DETECTOR_RAIL)
                || stack.is(Items.ACTIVATOR_RAIL);
    }

    private static int findFlintAndSteelSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.FLINT_AND_STEEL)) {
                return i;
            }
        }
        return -1;
    }

    private static int findRailSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (isRail(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findMinecartSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.TNT_MINECART)) {
                return i;
            }
        }
        return -1;
    }

    private void cleanup(Minecraft client) {

        predictions.entrySet().removeIf(entry -> {
            Entity entity = client.level.getEntity(entry.getKey());
            return !(entity instanceof Arrow);
        });

        processed.removeIf(id -> {
            Entity entity = client.level.getEntity(id);
            return !(entity instanceof Arrow);
        });
    }
}
