package com.zephyr.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Client-side A* pathfinder used by the {@link Pathing} module. Walks the block
 * grid between a start and a goal, expanding standable positions where the feet
 * block is passable and the block below is solid. Horizontal steps are cost 1,
 * one-block climbs cost a little more, and short falls (up to {@value #MAX_FALL}
 * blocks) are allowed so the path can drop off ledges. Lava is never entered.
 */
public final class AStarPathfinder {
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final int MAX_STEPS = 20_000;
    private static final int MAX_FALL = 3;

    private AStarPathfinder() {
    }

    /**
     * Computes a path of standable block positions from {@code start} to
     * {@code goal}, or an empty list when no route exists (or the search is
     * aborted by {@value #MAX_STEPS}). Both endpoints are snapped to the nearest
     * standable position so coordinates inside walls still resolve.
     *
     * @param client the Minecraft client instance
     * @param start  the starting block position
     * @param goal   the target block position
     * @return the ordered path, from start to goal, or an empty list if unreachable
     */
    public static List<BlockPos> findPath(Minecraft client, BlockPos start, BlockPos goal) {
        if (client.level == null) return List.of();
        BlockPos startStand = nearestStandable(client, start);
        if (startStand == null) return List.of();
        BlockPos goalStand = nearestStandable(client, goal);
        if (goalStand == null) return List.of();

        Map<BlockPos, Double> gScore = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));

        gScore.put(startStand, 0.0);
        open.add(new Node(startStand, 0.0, heuristic(startStand, goalStand)));

        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_STEPS) {
            Node current = open.poll();
            if (current == null) break;
            if (closed.contains(current.pos)) continue;
            if (current.g > gScore.getOrDefault(current.pos, Double.MAX_VALUE)) continue;
            closed.add(current.pos);
            expanded++;

            if (current.pos.equals(goalStand)) {
                return reconstruct(cameFrom, goalStand);
            }

            for (BlockPos next : neighbors(client, current.pos)) {
                if (closed.contains(next)) continue;
                double tentative = current.g + stepCost(current.pos, next);
                if (tentative < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                    gScore.put(next, tentative);
                    cameFrom.put(next, current.pos);
                    open.add(new Node(next, tentative, tentative + heuristic(next, goalStand)));
                }
            }
        }
        return List.of();
    }

    /** Generates the walkable neighbors of a feet-level position. */
    private static List<BlockPos> neighbors(Minecraft client, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(12);
        for (Direction dir : HORIZONTALS) {
            BlockPos front = pos.relative(dir);

            if (isStandable(client, front)) {
                result.add(front);
            }

            BlockPos stepUp = front.above();
            if (isStandable(client, stepUp) && isPassable(client, stepUp.above())) {
                result.add(stepUp);
            }

            for (int drop = 1; drop <= MAX_FALL; drop++) {
                BlockPos down = front.below(drop);
                if (!isPassable(client, down)) break;
                if (isStandable(client, down)) {
                    result.add(down);
                    break;
                }
            }
        }
        return result;
    }

    private static double stepCost(BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();
        if (dy > 0) return 1.3;
        if (dy < 0) return 1.0 + 0.1 * -dy;
        return 1.0;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** A position the player can stand on: feet passable, block below solid (not lava). */
    private static boolean isStandable(Minecraft client, BlockPos feet) {
        if (!isPassable(client, feet)) return false;
        BlockState below = state(client, feet.below());
        if (below.isAir()) return false;
        return !isLava(client, feet.below());
    }

    /** A block the player may occupy or walk through (air, replaceable blocks, liquids). */
    private static boolean isPassable(Minecraft client, BlockPos pos) {
        BlockState state = state(client, pos);
        return (state.isAir() || state.canBeReplaced()) && !isLava(client, pos);
    }

    private static boolean isLava(Minecraft client, BlockPos pos) {
        return client.level.getFluidState(pos).is(FluidTags.LAVA);
    }

    private static BlockState state(Minecraft client, BlockPos pos) {
        return client.level.getBlockState(pos);
    }

    /** Snaps a possibly buried position to the closest standable spot, or {@code null}. */
    private static BlockPos nearestStandable(Minecraft client, BlockPos pos) {
        if (isStandable(client, pos)) return pos;
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos candidate = pos.above(dy);
            if (isStandable(client, candidate)) return candidate;
        }
        for (Direction dir : HORIZONTALS) {
            BlockPos candidate = pos.relative(dir);
            if (isStandable(client, candidate)) return candidate;
            if (isStandable(client, candidate.above())) return candidate.above();
            if (isStandable(client, candidate.below())) return candidate.below();
        }
        return null;
    }

    private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> cameFrom, BlockPos end) {
        Deque<BlockPos> path = new ArrayDeque<>();
        BlockPos current = end;
        while (current != null) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }
        return new ArrayList<>(path);
    }

    private record Node(BlockPos pos, double g, double f) {
    }
}