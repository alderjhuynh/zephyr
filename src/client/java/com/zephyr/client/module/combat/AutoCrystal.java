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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Combat module that places and detonates end crystals on a target's obsidian support. End
 * crystals can only be placed on obsidian or bedrock with empty space above, so the module
 * first places an obsidian block beside the target when one is missing, then a crystal on
 * top of it, and finally attacks the crystal to detonate it once the target is in the blast.
 * A small action delay paces the place/place/detonate sequence so the server registers each
 * step. Honors the {@code Reach} module for both block and entity interaction ranges.
 */
public final class AutoCrystal extends Module {
    public static final AutoCrystal INSTANCE = new AutoCrystal();

    /** End crystal explosion radius, in blocks. */
    private static final double CRYSTAL_BLAST_RADIUS = 6.0;

    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", false);
    private final BooleanSetting placeObsidian = new BooleanSetting("Place Obsidian", true);
    private final BooleanSetting avoidSelfDamage = new BooleanSetting("Avoid Self Damage", true);
    private final NumberSetting actionDelay = new NumberSetting("Action Delay", 2D, 0D, 10D, 1D);

    private BlockPos activeSupport;
    private int waitTicks;

    private AutoCrystal() {
        super("Auto Crystal", "Places and detonates end crystals on a target's obsidian support", Category.COMBAT);
        addSetting(playersOnly);
        addSetting(placeObsidian);
        addSetting(avoidSelfDamage);
        addSetting(actionDelay);
    }

    /** Drives the place-obsidian/place-crystal/detonate state machine each tick. */
    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null || client.level == null) return;

        LocalPlayer player = client.player;
        LivingEntity target = findTarget(client);
        if (target == null) {
            activeSupport = null;
            waitTicks = 0;
            return;
        }

        BlockPos support = findSupportPos(client, target);
        if (support == null) return;

        if (support.equals(activeSupport) && waitTicks > 0) {
            waitTicks--;
            return;
        }

        BlockPos crystalPos = support.above();
        EndCrystal crystal = findCrystalAt(client, crystalPos);
        if (crystal != null) {
            if (shouldDetonate(player, target, crystal)) {
                detonate(client, crystal);
            }
            activeSupport = null;
            waitTicks = 0;
            return;
        }

        BlockState supportState = client.level.getBlockState(support);
        if (!isCrystalSupport(supportState)) {
            if (!placeObsidian.get()) return;
            placeObsidian(client, player, support);
            scheduleNext(support);
            return;
        }

        placeCrystal(client, player, support);
        scheduleNext(support);
    }

    private void scheduleNext(BlockPos support) {
        activeSupport = support;
        waitTicks = (int) Math.round(actionDelay.get());
    }

    /**
     * Whether the crystal may be detonated. With {@code avoidSelfDamage} disabled this is
     * always true. Otherwise the blast is only held back when the player would take more of
     * the explosion than the target, i.e. the player is inside the blast radius and standing
     * closer to the crystal than the target is.
     */
    private boolean shouldDetonate(LocalPlayer player, LivingEntity target, EndCrystal crystal) {
        if (!avoidSelfDamage.get()) return true;

        double blastSq = CRYSTAL_BLAST_RADIUS * CRYSTAL_BLAST_RADIUS;
        double playerDistanceSq = player.distanceToSqr(crystal.position());
        if (playerDistanceSq > blastSq) return true;

        return target.distanceToSqr(crystal.position()) <= playerDistanceSq;
    }

    private void detonate(Minecraft client, EndCrystal crystal) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        double reach = entityRange(player);
        if (player.distanceToSqr(crystal) > reach * reach) return;

        client.gameMode.attack(player, crystal);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private void placeObsidian(Minecraft client, LocalPlayer player, BlockPos support) {
        if (client.gameMode == null) return;

        int slot = findSlot(player, Items.OBSIDIAN, 1);
        if (slot == -1) return;

        BlockHitResult hit = findPlaceHit(client, support);
        if (hit == null) return;

        int previous = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.getInventory().setSelectedSlot(previous);
    }

    private void placeCrystal(Minecraft client, LocalPlayer player, BlockPos support) {
        if (client.gameMode == null) return;

        int slot = findSlot(player, Items.END_CRYSTAL, 1);
        if (slot == -1) return;

        int previous = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
        player.getInventory().setSelectedSlot(previous);
    }

    /** Picks the obsidian support position next to the target, preferring the spot already being worked on. */
    private static BlockPos findSupportPos(Minecraft client, LivingEntity target) {
        LocalPlayer player = client.player;
        double range = entityRange(player);
        double rangeSq = range * range;

        BlockPos bestSupport = null;
        BlockPos bestPlaceable = null;
        double bestSupportDistanceSq = Double.MAX_VALUE;
        double bestPlaceableDistanceSq = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = target.blockPosition().relative(direction);
            if (candidate.equals(player.blockPosition())) continue;

            double distanceSq = player.distanceToSqr(Vec3.atCenterOf(candidate.above()));
            if (distanceSq > rangeSq) continue;

            BlockState state = client.level.getBlockState(candidate);
            if (isCrystalSupport(state)) {
                if (distanceSq < bestSupportDistanceSq) {
                    bestSupportDistanceSq = distanceSq;
                    bestSupport = candidate;
                }
            } else if (state.isAir() || state.canBeReplaced()) {
                if (distanceSq < bestPlaceableDistanceSq) {
                    bestPlaceableDistanceSq = distanceSq;
                    bestPlaceable = candidate;
                }
            }
        }

        if (bestSupport != null) return bestSupport;
        return bestPlaceable;
    }

    private static EndCrystal findCrystalAt(Minecraft client, BlockPos pos) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;
            if (crystal.blockPosition().equals(pos)) return crystal;
        }
        return null;
    }

    /** Whether a block can support an end crystal on top of it. */
    private static boolean isCrystalSupport(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    /** Finds a neighboring solid block to click so a block is placed at the given position. */
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

    private static LivingEntity findTarget(Minecraft client) {
        LocalPlayer player = client.player;
        double range = entityRange(player);
        double rangeSq = range * range;

        LivingEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == player || !living.isAlive() || living.isSpectator()) continue;
            if (AutoCrystal.INSTANCE.playersOnly.get() && !(living instanceof Player)) continue;

            double distanceSq = player.distanceToSqr(living);
            if (distanceSq > rangeSq) continue;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = living;
            }
        }
        return best;
    }

    private static int findSlot(LocalPlayer player, Item item, int minCount) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item) && stack.getCount() >= minCount) return i;
        }
        return -1;
    }

    /** The player's entity interaction range, extended by the {@code Reach} module. */
    private static double entityRange(LocalPlayer player) {
        double range = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            range += Reach.INSTANCE.entityReach.get();
        }
        return range;
    }
}
