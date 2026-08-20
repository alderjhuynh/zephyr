package com.zephyr.client.module.bots.pathing;

import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.List;
import java.util.Set;

/**
 * Movement module that walks the player to a set of coordinates using
 * {@link AStarPathfinder}. Each tick it steers the player toward the next
 * waypoint of the A* route and exposes {@link #wantsJump()} so the pathing
 * {@code KeyboardInput} mixin can force forward movement and jumping. Set the
 * destination with {@code .z path <x> <y> <z>}; disabling the module stops the
 * walk.
 *
 * <p>When started with {@link #startPath(BlockPos, boolean)} and destructive
 * enabled, the route may carve through mineable walls and bridge gaps. While
 * walking, the module breaks the reported {@code blocksToMine} by holding a
 * destroy action on the nearest reachable one, and fills the reported
 * {@code blocksToPlace} support positions with whatever block item is in the
 * hotbar (including blocks picked up from mining).
 */
public final class Pathing extends Module {
    public static final Pathing INSTANCE = new Pathing();

    private static final int RECOMPUTE_INTERVAL = 40;
    private static final double WAYPOINT_RADIUS = 0.7;
    private static final double INTERACTION_RANGE = 4.0;

    private BlockPos target;
    private List<BlockPos> path = new ArrayList<>();
    private Set<BlockPos> blocksToMine = new HashSet<>();
    private Set<BlockPos> blocksToPlace = new HashSet<>();
    private double routeCost;
    private int pathIndex;
    private int recomputeTimer;
    private boolean wantsJump;
    private boolean destructive;
    private boolean mining;
    private BlockPos miningTarget;
    private Direction miningDirection;
    private boolean noPlacementBlocksReported;
    private Block taskBlock;
    private String taskBlockId;
    private BlockPos taskTarget;

    private Pathing() {
        super("Pathing", "Walks to a set of coordinates using A* pathfinding", Category.MOVEMENT);
    }

    /**
     * Sets the destination, clears the current route and enables the module.
     * The path itself is computed lazily on the next tick.
     *
     * @param target the block position to walk to
     */
    public void startPath(BlockPos target) {
        startPath(target, false);
    }

    /**
     * Sets the destination and enables the module, optionally allowing
     * destructive pathing (mining through walls and bridging gaps). When in a
     * world, the route is computed immediately and returned so the calling
     * command can show cost and edit counts; otherwise it is computed on the
     * next tick.
     *
     * @param target      the block position to walk to
     * @param destructive whether mining and block placement are allowed
     * @return the computed route plan, or {@code null} if not in a world
     */
    public AStarPathfinder.PathResult startPath(BlockPos target, boolean destructive) {
        return begin(target, destructive, null, null);
    }

    /**
     * Starts a mine task: walks to {@code target} (destructive, so the route may
     * dig through walls and bridge gaps) and breaks the {@code block} sitting at
     * that position. The module finishes and disables itself once the block is
     * mined.
     *
     * @param target  the position of the block to mine
     * @param block   the block type to break
     * @param blockId the block's registry id, used for progress messages
     * @return the computed route plan, or {@code null} if not in a world
     */
    public AStarPathfinder.PathResult startTask(BlockPos target, Block block, String blockId) {
        return begin(target, true, block, blockId);
    }

    private AStarPathfinder.PathResult begin(BlockPos target, boolean destructive, Block taskBlock, String taskBlockId) {
        this.target = target;
        this.destructive = destructive;
        this.taskBlock = taskBlock;
        this.taskBlockId = taskBlockId;
        this.taskTarget = taskBlock != null ? target.immutable() : null;
        this.path = new ArrayList<>();
        this.blocksToMine = new HashSet<>();
        this.blocksToPlace = new HashSet<>();
        this.routeCost = 0.0;
        this.pathIndex = 0;
        this.recomputeTimer = 0;
        this.wantsJump = false;
        this.mining = false;
        this.miningTarget = null;
        this.miningDirection = null;
        this.noPlacementBlocksReported = false;

        Minecraft client = Minecraft.getInstance();
        AStarPathfinder.PathResult result = null;
        if (client.player != null && client.level != null) {
            result = AStarPathfinder.findPath(client, client.player.blockPosition(), target, destructive);
            this.path = result.path();
            this.blocksToMine = result.blocksToMine();
            this.blocksToPlace = result.blocksToPlace();
            this.routeCost = result.cost();
            this.recomputeTimer = RECOMPUTE_INTERVAL;
        }

        if (!isEnabled()) {
            setEnabled(true);
        }
        return result;
    }

    /** Whether the module currently has a walkable route it is following. */
    public boolean isActive() {
        return target != null && !path.isEmpty();
    }

    /** The destination block the bot is walking to, or {@code null}. */
    public BlockPos getTarget() {
        return target;
    }

    /** Whether the current walk is a mine task with a target block. */
    public boolean isTask() {
        return taskBlock != null;
    }

    /** The registry id of the block being mined by the current task. */
    public String getTaskBlockId() {
        return taskBlockId;
    }

    /** The position of the block the current task must break, or {@code null}. */
    public BlockPos getTaskTarget() {
        return taskTarget;
    }

    /** The current A* route, from start to goal (may be empty). */
    public List<BlockPos> getPath() {
        return path;
    }

    /** Index of the next waypoint the bot is heading toward. */
    public int getPathIndex() {
        return pathIndex;
    }

    /** Whether the input mixin should hold the jump key on the next tick. */
    public boolean wantsJump() {
        return wantsJump;
    }

    /** Whether the current route may mine and place blocks. */
    public boolean isDestructive() {
        return destructive;
    }

    /** Solid blocks the current route plans to break. */
    public Set<BlockPos> getBlocksToMine() {
        return blocksToMine;
    }

    /** Support positions the current route plans to fill with placed blocks. */
    public Set<BlockPos> getBlocksToPlace() {
        return blocksToPlace;
    }

    /** Total A* cost of the current route. */
    public double getRouteCost() {
        return routeCost;
    }

    /**
     * Whether the walk should hold still: used when a gap must be bridged but
     * there is no placeable block in the hotbar, so the bot does not walk off
     * the edge.
     */
    public boolean wantsPause() {
        if (!destructive || blocksToPlace.isEmpty()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        return findPlaceableSlot(client.player.getInventory()) == -1;
    }

    @Override
    protected void onDisable() {
        if (mining) {
            Minecraft client = Minecraft.getInstance();
            if (client.gameMode != null) {
                client.gameMode.stopDestroyBlock();
            }
        }
        target = null;
        path = new ArrayList<>();
        blocksToMine = new HashSet<>();
        blocksToPlace = new HashSet<>();
        pathIndex = 0;
        wantsJump = false;
        destructive = false;
        mining = false;
        miningTarget = null;
        miningDirection = null;
        noPlacementBlocksReported = false;
        taskBlock = null;
        taskBlockId = null;
        taskTarget = null;
    }

    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || target == null) return;

        if (taskBlock != null) {
            if (!isTaskBlockPresent(client)) {
                CommandManager.sendMessage("Task complete: mined " + taskBlockId + " at " + target.toShortString());
                setEnabled(false);
                return;
            }
        } else if (reached(player, target)) {
            CommandManager.sendMessage("Pathing: reached " + target.toShortString());
            setEnabled(false);
            return;
        }

        recomputeTimer--;
        boolean shouldRecompute = recomputeTimer <= 0 || path.isEmpty() || pathIndex >= path.size();
        if (!shouldRecompute) {
            shouldRecompute = distanceTo(player, path.get(pathIndex)) > 5.0;
        }
        if (shouldRecompute) {
            AStarPathfinder.PathResult result = AStarPathfinder.findPath(client, player.blockPosition(), target, destructive);
            path = result.path();
            blocksToMine = result.blocksToMine();
            blocksToPlace = result.blocksToPlace();
            routeCost = result.cost();
            pathIndex = 0;
            recomputeTimer = RECOMPUTE_INTERVAL;
            if (path.isEmpty()) {
                if (taskBlock != null) {
                    CommandManager.sendMessage("Task: no path to " + taskBlockId + " at " + target.toShortString());
                } else {
                    CommandManager.sendMessage("Pathing: no path found to " + target.toShortString());
                }
                setEnabled(false);
                return;
            }
        }

        handleMining(client, player);
        handlePlacing(client, player);

        while (pathIndex < path.size() && closeTo(player, path.get(pathIndex)) && canAdvancePast(path.get(pathIndex))) {
            pathIndex++;
        }

        if (pathIndex >= path.size()) {
            if (taskBlock == null) {
                setEnabled(false);
                return;
            }
            // The task block still needs breaking: hold next to it and let
            // handleMining finish the job while the input mixin keeps pushing
            // forward into the wall.
            player.setYRot(yawTo(player.getX(), player.getZ(), target.getX() + 0.5, target.getZ() + 0.5));
            wantsJump = false;
            return;
        }

        BlockPos next = path.get(pathIndex);
        player.setYRot(yawTo(player.getX(), player.getZ(), next.getX() + 0.5, next.getZ() + 0.5));

        wantsJump = next.getY() > player.blockPosition().getY() || isBlockedAhead(client, player);
    }

    /**
     * Breaks the nearest mine target within reach. Uses
     * {@link MultiPlayerGameMode#continueDestroyBlock} which starts, advances
     * and finally breaks the block client-side (sending the matching packets).
     */
    private void handleMining(Minecraft client, LocalPlayer player) {
        if (!destructive) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        if (mining && miningTarget != null) {
            if (!isMineable(client, miningTarget)) {
                gameMode.stopDestroyBlock();
                mining = false;
                blocksToMine.remove(miningTarget);
                miningTarget = null;
                miningDirection = null;
            } else if (distanceSq(player, miningTarget) > INTERACTION_RANGE * INTERACTION_RANGE) {
                // Out of reach again (e.g. knocked back): stop for now and retry
                // once the bot closes back in.
                gameMode.stopDestroyBlock();
                mining = false;
                miningTarget = null;
                miningDirection = null;
            }
        }

        BlockPos mineTarget = pickMineTarget(client, player);
        if (mineTarget == null) {
            if (mining) {
                gameMode.stopDestroyBlock();
                mining = false;
                miningTarget = null;
                miningDirection = null;
            }
            return;
        }

        if (!mining || !mineTarget.equals(miningTarget)) {
            if (mining) gameMode.stopDestroyBlock();
            miningDirection = faceToward(mineTarget, player);
        }
        mining = true;
        miningTarget = mineTarget;
        lookAt(client, player, mineTarget);
        gameMode.continueDestroyBlock(mineTarget, miningDirection);

        if (client.level.getBlockState(mineTarget).isAir()) {
            blocksToMine.remove(mineTarget);
            mining = false;
            miningTarget = null;
            miningDirection = null;
        }
    }

    /**
     * Fills the nearest support position within reach with a block from the
     * hotbar, aiming at the top of the block below the support so the placed
     * block lands exactly at the support position.
     */
    private void handlePlacing(Minecraft client, LocalPlayer player) {
        if (!destructive || blocksToPlace.isEmpty()) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        BlockPos placeTarget = pickPlaceTarget(client, player);
        if (placeTarget == null) return;

        Inventory inv = player.getInventory();
        int slot = findPlaceableSlot(inv);
        if (slot == -1) {
            if (!noPlacementBlocksReported) {
                CommandManager.sendMessage("Pathing: no placeable block in hotbar to bridge the gap");
                noPlacementBlocksReported = true;
            }
            return;
        }
        noPlacementBlocksReported = false;

        int previous = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        lookAt(client, player, placeTarget.below());
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(placeTarget.below()).add(0.0, 0.5, 0.0),
                Direction.UP,
                placeTarget.below(),
                false);
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(previous);

        // Only drop the target once the block is actually present client-side, so
        // a rejected placement is retried instead of walking off the edge.
        if (isSolid(client, placeTarget)) {
            blocksToPlace.remove(placeTarget);
        }
    }

    /** Returns the nearest unreachable-skipped mine target, pruning cleared blocks. */
    private BlockPos pickMineTarget(Minecraft client, LocalPlayer player) {
        pruneMineTargets(client);

        // A task's target block is the priority: break it as soon as it is in reach.
        if (taskTarget != null && isTaskBlockPresent(client)
                && isMineable(client, taskTarget)
                && distanceSq(player, taskTarget) <= INTERACTION_RANGE * INTERACTION_RANGE) {
            return taskTarget;
        }

        // Prefer the block blocking the current waypoint so the walk does not
        // wander off digging sideways while a wall is still in the way.
        BlockPos cur = pathIndex < path.size() ? path.get(pathIndex) : null;
        if (cur != null) {
            for (BlockPos candidate : new BlockPos[]{cur, cur.above()}) {
                if (blocksToMine.contains(candidate)
                        && distanceSq(player, candidate) <= INTERACTION_RANGE * INTERACTION_RANGE) {
                    return candidate;
                }
            }
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : blocksToMine) {
            double d = distanceSq(player, pos);
            if (d <= INTERACTION_RANGE * INTERACTION_RANGE && d < bestDist) {
                best = pos;
                bestDist = d;
            }
        }
        return best;
    }

    /** Drops mine targets that have already been cleared or become unbreakable. */
    private void pruneMineTargets(Minecraft client) {
        blocksToMine.removeIf(pos -> !isMineable(client, pos));
    }

    /** Returns the nearest place target within reach, pruning already-filled positions. */
    private BlockPos pickPlaceTarget(Minecraft client, LocalPlayer player) {
        // Drop targets that are already solid (placed or filled in by the world).
        blocksToPlace.removeIf(pos -> isSolid(client, pos));

        // Prefer the support under the current waypoint.
        BlockPos cur = pathIndex < path.size() ? path.get(pathIndex) : null;
        if (cur != null && blocksToPlace.contains(cur.below())
                && distanceSq(player, cur.below()) <= INTERACTION_RANGE * INTERACTION_RANGE) {
            return cur.below();
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : blocksToPlace) {
            double d = distanceSq(player, pos);
            if (d <= INTERACTION_RANGE * INTERACTION_RANGE && d < bestDist) {
                best = pos;
                bestDist = d;
            }
        }
        return best;
    }

    /** First hotbar slot holding a placeable block item, or -1. */
    private static int findPlaceableSlot(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private static double distanceSq(LocalPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** The face of the block pointing toward the player, for breaking. */
    private static Direction faceToward(BlockPos pos, LocalPlayer player) {
        return Direction.getNearest(player.blockPosition().subtract(pos), Direction.UP);
    }

    /**
     * Transiently aims the player at a block for a mining or placing action,
     * then restores the previous rotation so the camera does not get left
     * pointing into the ground after digging.
     */
    private static void lookAt(Minecraft client, LocalPlayer player, BlockPos pos) {
        float prevYaw = player.getYRot();
        float prevPitch = player.getXRot();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
        player.setYRot(prevYaw);
        player.setXRot(prevPitch);
    }

    /** Whether the player has arrived near the destination. In destructive mode
     * the vertical tolerance is tight so a goal below the bot is actually dug to,
     * not "reached" while still several blocks above it. */
    private boolean reached(LocalPlayer player, BlockPos target) {
        double dx = player.getX() - (target.getX() + 0.5);
        double dz = player.getZ() - (target.getZ() + 0.5);
        double dy = player.getY() - target.getY();
        double dyTolerance = destructive ? 1.0 : 2.5;
        return dx * dx + dz * dz < 1.5 * 1.5 && Math.abs(dy) < dyTolerance;
    }

    /**
     * Whether the bot may advance past a waypoint without first acting on it:
     * a waypoint whose feet or head block still needs mining, or whose support
     * block still needs placing, holds the walk until the edit is done.
     */
    private boolean canAdvancePast(BlockPos pos) {
        if (blocksToMine.contains(pos) || blocksToMine.contains(pos.above())) return false;
        return !blocksToPlace.contains(pos.below());
    }

    /** Whether the player is close enough to a waypoint to advance past it. */
    private static boolean closeTo(LocalPlayer player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double dy = player.getY() - pos.getY();
        return dx * dx + dz * dz < WAYPOINT_RADIUS * WAYPOINT_RADIUS && Math.abs(dy) < 2.0;
    }

    private static double distanceTo(LocalPlayer player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Whether a solid block sits directly in front of the player, so it should jump. */
    private static boolean isBlockedAhead(Minecraft client, LocalPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        for (double d = 0.2; d <= 1.4; d += 0.2) {
            BlockPos check = BlockPos.containing(px + dx * d, py + 0.3, pz + dz * d);
            if (isBlocked(client, check)) return true;
        }
        return false;
    }

    private static boolean isBlocked(Minecraft client, BlockPos pos) {
        return !client.level.getBlockState(pos).isAir()
                && !client.level.getBlockState(pos).canBeReplaced();
    }

    private static boolean isSolid(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced() && !isLava(client, pos);
    }

    private static boolean isMineable(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || isLava(client, pos)) return false;
        return state.getDestroySpeed(client.level, pos) != -1.0f;
    }

    /** Whether the current task's target block is still in place (not yet mined). */
    private boolean isTaskBlockPresent(Minecraft client) {
        return taskTarget != null && taskBlock != null
                && client.level.getBlockState(taskTarget).getBlock() == taskBlock;
    }

    private static boolean isLava(Minecraft client, BlockPos pos) {
        return client.level.getFluidState(pos).is(FluidTags.LAVA);
    }

    /** Minecraft yaw (degrees) that makes an observer at {@code (fromX, fromZ)} face {@code (toX, toZ)}. */
    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) Math.toDegrees(Math.atan2(fromX - toX, toZ - fromZ));
    }
}