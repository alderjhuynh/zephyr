package com.zephyr.client.module.bots.pathing;

import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Movement module that walks the player to a set of coordinates using
 * {@link AStarPathfinder}. Each tick it steers the player toward the next
 * waypoint of the A* route and exposes {@link #wantsJump()} so the pathing
 * {@code KeyboardInput} mixin can force forward movement and jumping. Set the
 * destination with {@code .z path <x> <y> <z>}; disabling the module stops the
 * walk.
 */
public final class Pathing extends Module {
    public static final Pathing INSTANCE = new Pathing();

    private static final int RECOMPUTE_INTERVAL = 40;
    private static final double WAYPOINT_RADIUS = 0.7;

    private BlockPos target;
    private List<BlockPos> path = new ArrayList<>();
    private int pathIndex;
    private int recomputeTimer;
    private boolean wantsJump;

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
        this.target = target;
        this.path = new ArrayList<>();
        this.pathIndex = 0;
        this.recomputeTimer = 0;
        this.wantsJump = false;
        if (!isEnabled()) {
            setEnabled(true);
        }
    }

    /** Whether the module currently has a walkable route it is following. */
    public boolean isActive() {
        return target != null && !path.isEmpty();
    }

    /** Whether the input mixin should hold the jump key on the next tick. */
    public boolean wantsJump() {
        return wantsJump;
    }

    @Override
    protected void onDisable() {
        target = null;
        path = new ArrayList<>();
        pathIndex = 0;
        wantsJump = false;
    }

    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || target == null) return;

        if (reached(player, target)) {
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
            path = AStarPathfinder.findPath(client, player.blockPosition(), target);
            pathIndex = 0;
            recomputeTimer = RECOMPUTE_INTERVAL;
            if (path.isEmpty()) {
                CommandManager.sendMessage("Pathing: no path found to " + target.toShortString());
                setEnabled(false);
                return;
            }
        }

        while (pathIndex < path.size() && closeTo(player, path.get(pathIndex))) {
            pathIndex++;
        }

        if (pathIndex >= path.size()) {
            setEnabled(false);
            return;
        }

        BlockPos next = path.get(pathIndex);
        player.setYRot(yawTo(player.getX(), player.getZ(), next.getX() + 0.5, next.getZ() + 0.5));

        wantsJump = next.getY() > player.blockPosition().getY() || isBlockedAhead(client, player);
    }

    /** Whether the player has arrived near the destination. */
    private static boolean reached(LocalPlayer player, BlockPos target) {
        double dx = player.getX() - (target.getX() + 0.5);
        double dz = player.getZ() - (target.getZ() + 0.5);
        double dy = player.getY() - target.getY();
        return dx * dx + dz * dz < 1.5 * 1.5 && Math.abs(dy) < 2.5;
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

    /** Minecraft yaw (degrees) that makes an observer at {@code (fromX, fromZ)} face {@code (toX, toZ)}. */
    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) Math.toDegrees(Math.atan2(fromX - toX, toZ - fromZ));
    }
}