package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Movement module that places a block under the player's feet while walking, bridging gaps
 * automatically. Each tick it looks for a block item in the hotbar and, when the block below
 * the feet is air (optionally), uses it against a neighboring solid block so a block is placed
 * beneath the player. The support block does not have to be directly underneath the target;
 * any adjacent solid block works, which allows horizontal bridging.
 */
public final class Scaffold extends Module {
    public static final Scaffold INSTANCE = new Scaffold();

    private final BooleanSetting onlyWhenAirBelow = new BooleanSetting("Only When Air Below", true);
    private final BooleanSetting swing = new BooleanSetting("Swing", true);

    private Scaffold() {
        super("Scaffold", "Places a block under your feet while you walk", Category.MOVEMENT);
        addSetting(onlyWhenAirBelow);
        addSetting(swing);
    }

    /** Places a block beneath the player each tick while the module is enabled. */
    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.level == null) return;

        BlockPos below = player.blockPosition().below();
        if (below.getY() < client.level.getMinY()) return;

        BlockState state = client.level.getBlockState(below);
        if (onlyWhenAirBelow.get() && !state.isAir() && !state.canBeReplaced()) return;

        BlockHitResult hit = findPlaceHit(client, below);
        if (hit == null) return;

        int slot = findBlockSlot(player);
        if (slot == -1) return;

        int previous = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.getInventory().setSelectedSlot(previous);

        if (swing.get()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Finds a neighboring solid block to click so a block is placed at the given position.
     * The support may sit below or beside the target, which lets the module scaffold
     * horizontally when there is nothing directly underneath.
     */
    private static BlockHitResult findPlaceHit(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return null;

        for (Direction direction : Direction.values()) {
            BlockPos supportPos = pos.relative(direction);
            BlockState support = client.level.getBlockState(supportPos);
            if (support.isAir() || !support.getFluidState().isEmpty()) continue;
            return new BlockHitResult(Vec3.atCenterOf(supportPos), direction.getOpposite(), supportPos, false);
        }
        return null;
    }

    /** Returns the first hotbar slot holding a block item, or -1. */
    private static int findBlockSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
