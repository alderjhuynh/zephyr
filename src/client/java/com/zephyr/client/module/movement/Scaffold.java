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

public final class Scaffold extends Module {
    public static final Scaffold INSTANCE = new Scaffold();

    private final BooleanSetting onlyWhenAirBelow = new BooleanSetting("Only When Air Below", true);
    private final BooleanSetting swing = new BooleanSetting("Swing", true);

    private Scaffold() {
        super("Scaffold", "Places a block under your feet while you walk", Category.MOVEMENT);
        addSetting(onlyWhenAirBelow);
        addSetting(swing);
    }

    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.level == null) return;

        BlockPos below = player.blockPosition().below();
        if (below.getY() < client.level.getMinBuildHeight()) return;

        BlockState state = client.level.getBlockState(below);
        if (onlyWhenAirBelow.get() && !state.isAir() && !state.canBeReplaced()) return;

        BlockHitResult hit = findPlaceHit(client, below);
        if (hit == null) return;

        int slot = findBlockSlot(player);
        if (slot == -1) return;

        int previous = player.getInventory().selected;
        player.getInventory().selected = slot;
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.getInventory().selected = previous;

        if (swing.get()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

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

    private static int findBlockSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
