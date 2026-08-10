package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AnchorAura extends Module {
    public static final AnchorAura INSTANCE = new AnchorAura();

    private AnchorAura() {
        super("AnchorAura", "Charges a respawn anchor in a target's face and detonates it, shielding yourself behind a glowstone block", Category.COMBAT);
        addSetting(playersOnly);
    }

    private final BooleanSetting playersOnly = new BooleanSetting("Player Only", false);

    /** Ticks to wait after the shield block is in place before detonating, so the server registers it. */
    private static final int SHIELD_DELAY = 2;
    /** Ticks to keep retrying the shield placement before detonating unprotected. */
    private static final int SHIELD_TIMEOUT = 10;

    private BlockPos pendingShield = null;
    private int shieldTicks = 0;

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null || client.level == null) return;

        LocalPlayer player = client.player;

        LivingEntity target = findTarget(client);
        if (target == null) {
            resetShield();
            return;
        }

        int anchorSlot = findSlot(player, Items.RESPAWN_ANCHOR, 1);
        int glowSlot = findSlot(player, Items.GLOWSTONE, 1);
        if (anchorSlot == -1 || glowSlot == -1) return;

        BlockPos anchorPos = findAnchorPos(client, target.blockPosition());
        if (anchorPos == null) return;

        int previousSlot = player.getInventory().selected;

        BlockState anchorState = client.level.getBlockState(anchorPos);

        if (!anchorState.is(Blocks.RESPAWN_ANCHOR)) {
            // Anchor not at the target's feet yet: place it.
            resetShield();
            BlockHitResult anchorHit = findSupport(client, anchorPos);
            if (anchorHit == null) {
                player.getInventory().selected = previousSlot;
                return;
            }
            player.getInventory().selected = anchorSlot;
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, anchorHit);
            player.getInventory().selected = previousSlot;
            return;
        }

        if (anchorState.getValue(RespawnAnchorBlock.CHARGE) == 0) {
            // Anchor placed but uncharged: charge it once. Hold onto a second
            // glowstone so we still have one left to shield ourselves with.
            resetShield();
            if (findSlot(player, Items.GLOWSTONE, 2) == -1) {
                player.getInventory().selected = previousSlot;
                return;
            }
            player.getInventory().selected = glowSlot;
            interact(client, anchorPos);
            player.getInventory().selected = previousSlot;
            return;
        }

        // Anchor is charged: shield ourselves behind a glowstone placed between
        // the anchor and the player, then detonate once it is in place.
        if (pendingShield == null) {
            pendingShield = findShieldPos(client, anchorPos, target.blockPosition());
            shieldTicks = 0;
            if (pendingShield == null) {
                // No room for a shield block between us and the anchor.
                detonate(client, player, anchorPos, glowSlot, previousSlot);
                return;
            }
        }

        if (!client.level.getBlockState(pendingShield).is(Blocks.GLOWSTONE)) {
            BlockHitResult shieldHit = findSupport(client, pendingShield);
            if (shieldHit != null) {
                player.getInventory().selected = glowSlot;
                client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, shieldHit);
            }
            player.getInventory().selected = previousSlot;
            if (++shieldTicks > SHIELD_TIMEOUT) {
                // Could not get the shield placed in time: detonate anyway.
                detonate(client, player, anchorPos, glowSlot, previousSlot);
            }
            return;
        }

        if (++shieldTicks < SHIELD_DELAY) {
            player.getInventory().selected = previousSlot;
            return;
        }

        detonate(client, player, anchorPos, glowSlot, previousSlot);
    }

    private void detonate(Minecraft client, LocalPlayer player, BlockPos anchorPos, int glowSlot, int previousSlot) {
        int detonateSlot = findDetonateSlot(player, glowSlot);
        if (detonateSlot != -1) {
            player.getInventory().selected = detonateSlot;
            interact(client, anchorPos);
        }
        player.getInventory().selected = previousSlot;
        resetShield();
    }

    private void resetShield() {
        pendingShield = null;
        shieldTicks = 0;
    }

    @Override
    protected void onDisable() {
        resetShield();
    }

    private LivingEntity findTarget(Minecraft client) {
        LocalPlayer player = client.player;
        double range = player.blockInteractionRange();
        if (Reach.INSTANCE.isEnabled()) {
            range += Reach.INSTANCE.blockReach.get();
        }
        double rangeSq = range * range;

        LivingEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == player || !living.isAlive() || living.isSpectator()) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;

            double distanceSq = player.distanceToSqr(living);
            if (distanceSq > rangeSq) continue;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = living;
            }
        }
        return best;
    }

    private static BlockPos findAnchorPos(Minecraft client, BlockPos targetPos) {
        LocalPlayer player = client.player;
        double range = player.blockInteractionRange();
        if (Reach.INSTANCE.isEnabled()) {
            range += Reach.INSTANCE.blockReach.get();
        }
        double rangeSq = range * range;

        BlockPos bestAnchor = null;
        BlockPos bestAir = null;
        double bestAnchorDistanceSq = Double.MAX_VALUE;
        double bestAirDistanceSq = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = targetPos.relative(direction);
            if (candidate.equals(player.blockPosition())) continue;

            double distanceSq = player.distanceToSqr(Vec3.atCenterOf(candidate));
            if (distanceSq > rangeSq) continue;

            BlockState state = client.level.getBlockState(candidate);
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                if (distanceSq < bestAnchorDistanceSq) {
                    bestAnchorDistanceSq = distanceSq;
                    bestAnchor = candidate;
                }
            } else if (state.isAir() || state.canBeReplaced()) {
                if (distanceSq < bestAirDistanceSq) {
                    bestAirDistanceSq = distanceSq;
                    bestAir = candidate;
                }
            }
        }
        // Prefer continuing an anchor we already placed over planting a new one.
        return bestAnchor != null ? bestAnchor : bestAir;
    }

    private static BlockPos findShieldPos(Minecraft client, BlockPos anchorPos, BlockPos targetPos) {
        LocalPlayer player = client.player;
        Vec3 anchorCenter = Vec3.atCenterOf(anchorPos);

        double dx = player.getX() - anchorCenter.x;
        double dz = player.getZ() - anchorCenter.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1.5) return null;

        double ux = dx / horizontalDist;
        double uz = dz / horizontalDist;

        // Walk along the horizontal line from the anchor toward the player,
        // picking the first placeable block on it (nearest the anchor gives
        // the best blast coverage).
        for (double dist = 1.0; dist <= horizontalDist - 0.5; dist += 0.5) {
            BlockPos candidate = BlockPos.containing(
                    anchorCenter.x + ux * dist,
                    anchorCenter.y,
                    anchorCenter.z + uz * dist
            );
            if (candidate.equals(anchorPos)
                    || candidate.equals(player.blockPosition())
                    || candidate.equals(targetPos)) continue;
            BlockState state = client.level.getBlockState(candidate);
            if (state.isAir() || state.canBeReplaced()) return candidate;
        }
        return null;
    }

    private static BlockHitResult findSupport(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return null;

        for (Direction direction : Direction.values()) {
            BlockPos supportPos = pos.relative(direction);
            BlockState support = client.level.getBlockState(supportPos);
            if (support.isAir() || !support.getFluidState().isEmpty()) continue;
            // Click the support block on the face facing `pos` so the placed
            // block lands exactly at `pos`.
            return new BlockHitResult(Vec3.atCenterOf(supportPos), direction.getOpposite(), supportPos, false);
        }
        return null;
    }

    private static void interact(Minecraft client, BlockPos pos) {
        client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        );
    }

    private static int findSlot(LocalPlayer player, Item item, int minCount) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item) && stack.getCount() >= minCount) return i;
        }
        return -1;
    }

    private static int findDetonateSlot(LocalPlayer player, int glowSlot) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i == glowSlot) continue;
            return i;
        }
        return -1;
    }
}
