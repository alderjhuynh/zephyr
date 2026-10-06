package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * While the use key is held (right click) with the crosshair on obsidian,
 * crying obsidian, or bedrock, continually places an end crystal on the
 * targeted block and attacks it, mirroring the {@code AutoCrystal} place and
 * detonate steps. Honors the {@code Reach} module via the player's interaction
 * ranges.
 */
public final class CrystalHelper extends Module {
    public static final CrystalHelper INSTANCE = new CrystalHelper();

    private CrystalHelper() {
        super("CrystalHelper", "While holding right click on obsidian, crying obsidian or bedrock, continually places and attacks end crystals", Category.COMBAT, false, false, true);
        addSetting(place);
        addSetting(attack);
        addSetting(actionDelay);
    }

    /** When enabled, places an end crystal on the targeted support block. */
    public final BooleanSetting place = new BooleanSetting("Place", true);

    /** When enabled, attacks the end crystal above the targeted support block. */
    public final BooleanSetting attack = new BooleanSetting("Attack", true);

    /** Ticks to wait between actions on the same support block so the server registers each step. */
    public final NumberSetting actionDelay = new NumberSetting("Action Delay", 1.0, 0.0, 10.0, 1.0);

    private BlockPos activeSupport;
    private int waitTicks;

    private int originalSlot = -1;
    private boolean holdingCrystals;

    /** Places and attacks a crystal each tick while right click is held on a valid support. */
    @Override
    public void tick(Minecraft client) {
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {
            return;
        }

        LocalPlayer player = client.player;

        if (!client.options.keyUse.isDown()) {
            releaseHeldSlot(player);
            reset();
            return;
        }

        BlockHitResult hit = pickSupportBlock(client, player);
        if (hit == null) {
            reset();
            return;
        }

        BlockPos supportPos = hit.getBlockPos();
        BlockState supportState = client.level.getBlockState(supportPos);
        if (!isCrystalSupport(supportState)) {
            reset();
            return;
        }

        double blockRange = player.blockInteractionRange();
        if (player.distanceToSqr(Vec3.atCenterOf(supportPos)) > blockRange * blockRange) {
            reset();
            return;
        }

        // Only lock to the crystal slot once targeting a valid in-range support,
        // so ordinary right-click uses (eating, bows, blocks, etc.) are untouched.
        ensureHoldingCrystals(player);

        if (supportPos.equals(activeSupport) && waitTicks > 0) {
            waitTicks--;
            return;
        }

        BlockPos crystalPos = supportPos.above();
        EndCrystal crystal = findCrystalAt(client, crystalPos);

        if (crystal != null) {
            if (attack.get()) {
                detonate(client, crystal);
            }
            scheduleNext(supportPos);
            return;
        }

        if (!place.get()) {
            reset();
            return;
        }

        if (!canPlaceCrystalAt(client, supportPos, crystalPos)) {
            reset();
            return;
        }

        placeCrystal(client, supportPos);
        scheduleNext(supportPos);
    }

    private void scheduleNext(BlockPos support) {
        activeSupport = support;
        waitTicks = actionDelay.get().intValue();
    }

    private void reset() {
        activeSupport = null;
        waitTicks = 0;
    }

    /** Resets the pacing state when the module is turned off. */
    @Override
    protected void onDisable() {
        reset();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            releaseHeldSlot(player);
        } else {
            originalSlot = -1;
            holdingCrystals = false;
        }
    }

    /**
     * Block-only pick that ignores entities, so an end crystal already sitting
     * on the support does not block targeting the support behind it.
     */
    private static BlockHitResult pickSupportBlock(Minecraft client, LocalPlayer player) {
        double blockRange = player.blockInteractionRange();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(blockRange));
        BlockHitResult hit = client.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return hit;
    }

    /** Locks the hotbar to the crystal slot while right click is held. */
    private void ensureHoldingCrystals(LocalPlayer player) {
        int crystalSlot = findCrystalSlot(player);
        if (crystalSlot == -1) return;
        if (!holdingCrystals) {
            originalSlot = player.getInventory().getSelectedSlot();
            holdingCrystals = true;
        }
        if (player.getInventory().getSelectedSlot() != crystalSlot) {
            player.getInventory().setSelectedSlot(crystalSlot);
        }
    }

    /** Restores the pre-hold hotbar slot once right click is released. */
    private void releaseHeldSlot(LocalPlayer player) {
        if (!holdingCrystals) return;
        holdingCrystals = false;
        int restore = originalSlot;
        originalSlot = -1;
        if (restore >= 0 && restore < 9
                && player.getInventory().getSelectedSlot() != restore) {
            player.getInventory().setSelectedSlot(restore);
        }
    }

    private static void detonate(Minecraft client, EndCrystal crystal) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        double reach = player.entityInteractionRange();
        if (player.distanceToSqr(crystal) > reach * reach) return;

        client.gameMode.attack(player, crystal);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
    }

    private void placeCrystal(Minecraft client, BlockPos support) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        if (!player.getInventory().getItem(player.getInventory().getSelectedSlot()).is(Items.END_CRYSTAL)) {
            int slot = findCrystalSlot(player);
            if (slot == -1) return;
            if (!holdingCrystals) {
                originalSlot = player.getInventory().getSelectedSlot();
                holdingCrystals = true;
            }
            player.getInventory().setSelectedSlot(slot);
        }

        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
    }

    /** Whether the targeted block should trigger crystal placement. */
    private static boolean isCrystalSupport(BlockState state) {
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.BEDROCK);
    }

    /** Whether a crystal can currently occupy the space above the support. */
    private static boolean canPlaceCrystalAt(Minecraft client, BlockPos support, BlockPos crystalPos) {
        BlockState above = client.level.getBlockState(crystalPos);
        if (!above.isAir() && !above.canBeReplaced()) return false;
        return findCrystalAt(client, crystalPos) == null;
    }

    private static EndCrystal findCrystalAt(Minecraft client, BlockPos pos) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;
            if (crystal.blockPosition().equals(pos)) return crystal;
        }
        return null;
    }

    private static int findCrystalSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }
}
