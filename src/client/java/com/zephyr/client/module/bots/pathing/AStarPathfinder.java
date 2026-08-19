package com.zephyr.client.module.bots.pathing;

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
 *
 * <p>When {@link #findPath(Minecraft, BlockPos, BlockPos, boolean)} is called
 * with {@code destructive} enabled the search can additionally carve through
 * mineable blocks and bridge gaps:
 * <ul>
 *   <li>Mining a solid block that blocks the way costs an extra
 *       {@value #MINE_COST} per block (a full wall mines the feet block and the
 *       block above it separately), and</li>
 *   <li>Stepping across a gap places a support block underneath, costing an
 *       extra {@value #PLACE_COST}.</li>
 * </ul>
 * The chosen route's mining and placement requirements are reported back through
 * {@link PathResult} so the {@code Pathing} module can act on them while walking.
 */
public final class AStarPathfinder {
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final int MAX_STEPS = 20_000;
    private static final int MAX_FALL = 3;

    /** Extra cost per block the player must mine to walk through a wall. */
    public static final double MINE_COST = 2.5;
    /** Extra cost per support block placed to bridge a gap. */
    public static final double PLACE_COST = 2.0;

    private AStarPathfinder() {
    }

    /**
     * A planned route plus the terrain edits (mining and placement) required to
     * walk it, along with the route's total cost.
     *
     * @param path          the walkable feet-level positions, from start to goal
     * @param blocksToMine  solid blocks that must be broken along the route
     * @param blocksToPlace empty positions that must be filled with a support block
     * @param cost          the total A* cost of the route
     */
    public record PathResult(
            List<BlockPos> path,
            Set<BlockPos> blocksToMine,
            Set<BlockPos> blocksToPlace,
            double cost) {
    }

    /**
     * Computes a non-destructive path; equivalent to
     * {@link #findPath(Minecraft, BlockPos, BlockPos, boolean)} with
     * {@code destructive} disabled. Returns just the route.
     *
     * @param client the Minecraft client instance
     * @param start  the starting block position
     * @param goal   the target block position
     * @return the ordered path, from start to goal, or an empty list if unreachable
     */
    public static List<BlockPos> findPath(Minecraft client, BlockPos start, BlockPos goal) {
        return findPath(client, start, goal, false).path();
    }

    /**
     * Computes a path of standable block positions from {@code start} to
     * {@code goal}, or an empty list when no route exists (or the search is
     * aborted by {@value #MAX_STEPS}). Both endpoints are snapped to the nearest
     * standable position so coordinates inside walls still resolve.
     *
     * <p>With {@code destructive} enabled, mineable blocks may be carved through
     * and gaps bridged with placed support blocks; the required edits are
     * reported in the returned {@link PathResult}.
     *
     * @param client      the Minecraft client instance
     * @param start       the starting block position
     * @param goal        the target block position
     * @param destructive whether mining and bridging are allowed
     * @return the route plan, or an empty one if unreachable
     */
    public static PathResult findPath(Minecraft client, BlockPos start, BlockPos goal, boolean destructive) {
        if (client.level == null) return empty();
        BlockPos startStand = nearestStandable(client, start);
        if (startStand == null) return empty();
        BlockPos goalStand = nearestStandable(client, goal);
        // In destructive mode the goal itself may be a block to dig into (e.g. a
        // target a few blocks under the surface).
        if (goalStand == null && destructive && isMineable(client, goal)) {
            goalStand = goal.immutable();
        }
        if (goalStand == null) return empty();

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
                return deriveActions(client, reconstruct(cameFrom, goalStand), current.g);
            }

            for (Edge edge : neighbors(client, current.pos, destructive)) {
                if (closed.contains(edge.pos)) continue;
                double tentative = current.g + edge.cost;
                if (tentative < gScore.getOrDefault(edge.pos, Double.MAX_VALUE)) {
                    gScore.put(edge.pos, tentative);
                    cameFrom.put(edge.pos, current.pos);
                    open.add(new Node(edge.pos, tentative, tentative + heuristic(edge.pos, goalStand)));
                }
            }
        }
        return empty();
    }

    /** Generates the walkable neighbors of a feet-level position, with their costs. */
    private static List<Edge> neighbors(Minecraft client, BlockPos pos, boolean destructive) {
        List<Edge> result = new ArrayList<>(12);
        for (Direction dir : HORIZONTALS) {
            addForwardEdges(client, result, pos.relative(dir), destructive);
        }
        if (destructive) {
            addVerticalEdges(client, result, pos);
        }
        return result;
    }

    /**
     * Adds the vertical moves in destructive mode: digging down by mining the
     * block under the feet, or falling straight down into an open hole.
     */
    private static void addVerticalEdges(Minecraft client, List<Edge> result, BlockPos pos) {
        // Digging down: mine the block under the feet and fall into the opening.
        if (isMineable(client, pos.below())) {
            result.add(new Edge(pos.below(), 1.0 + MINE_COST));
        }
        // Falling straight down into a hole (a landing must exist within MAX_FALL).
        for (int drop = 1; drop <= MAX_FALL; drop++) {
            BlockPos down = pos.below(drop);
            if (!isPassable(client, down)) break;
            if (isStandable(client, down)) {
                result.add(new Edge(down, 1.0 + 0.1 * drop));
                break;
            }
        }
    }

    /**
     * Adds every way to step from the current position onto {@code front} (one
     * block over horizontally): walking, mining through the wall, climbing up,
     * or dropping down.
     */
    private static void addForwardEdges(Minecraft client, List<Edge> result, BlockPos front, boolean destructive) {
        // Walking forward onto `front`.
        if (isStandable(client, front)) {
            if (!destructive || isPassable(client, front.above())) {
                result.add(new Edge(front, 1.0));
            } else if (isMineable(client, front.above())) {
                result.add(new Edge(front, 1.0 + MINE_COST));
            }
        }
        // Bridging: `front` is empty but there is no floor below, so a support
        // block gets placed underneath before stepping on it.
        else if (destructive && isPassable(client, front) && isPassable(client, front.below())) {
            double cost = 1.0 + PLACE_COST;
            if (isPassable(client, front.above())) {
                result.add(new Edge(front, cost));
            } else if (isMineable(client, front.above())) {
                result.add(new Edge(front, cost + MINE_COST));
            }
        }
        // Mining through the wall: `front` itself is solid but can be cleared.
        else if (destructive && isMineable(client, front) && isSolid(client, front.below())) {
            double cost = 1.0 + MINE_COST;
            if (isPassable(client, front.above())) {
                result.add(new Edge(front, cost));
            } else if (isMineable(client, front.above())) {
                result.add(new Edge(front, cost + MINE_COST));
            }
        }

        // Stepping up onto `front.above()`.
        BlockPos stepUp = front.above();
        if (isStandable(client, stepUp)) {
            if (!destructive || isPassable(client, stepUp.above())) {
                result.add(new Edge(stepUp, 1.3));
            } else if (isMineable(client, stepUp.above())) {
                result.add(new Edge(stepUp, 1.3 + MINE_COST));
            }
        }

        // Short falls onto `front.below(drop)`.
        for (int drop = 1; drop <= MAX_FALL; drop++) {
            BlockPos down = front.below(drop);
            if (!isPassable(client, down)) break;
            if (isStandable(client, down)) {
                result.add(new Edge(down, 1.0 + 0.1 * drop));
                break;
            }
        }
    }

    /**
     * Walks the reconstructed route and reports every block that must be mined
     * or placed for it to be walkable.
     */
    private static PathResult deriveActions(Minecraft client, List<BlockPos> path, double cost) {
        Set<BlockPos> toMine = new HashSet<>();
        Set<BlockPos> toPlace = new HashSet<>();
        for (int i = 1; i < path.size(); i++) {
            BlockPos cur = path.get(i);
            BlockPos prev = path.get(i - 1);
            // A support block is only placed when the step is horizontal (bridging);
            // waypoints reached by digging or falling keep their gap so the bot falls.
            if (cur.getY() == prev.getY()
                    && !isLava(client, cur.below()) && isPassable(client, cur.below())) {
                toPlace.add(cur.below().immutable());
            }
            if (isMineable(client, cur)) {
                toMine.add(cur.immutable());
            }
            if (isMineable(client, cur.above())) {
                toMine.add(cur.above().immutable());
            }
        }
        return new PathResult(path, toMine, toPlace, cost);
    }

    private static PathResult empty() {
        return new PathResult(List.of(), Set.of(), Set.of(), 0.0);
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

    /** A block that provides solid ground (cannot be walked through). */
    private static boolean isSolid(Minecraft client, BlockPos pos) {
        return !isPassable(client, pos) && !isLava(client, pos);
    }

    /** A solid block that can actually be broken by hand (not bedrock, barrier, etc.). */
    private static boolean isMineable(Minecraft client, BlockPos pos) {
        BlockState state = state(client, pos);
        if (state.isAir() || state.canBeReplaced() || isLava(client, pos)) return false;
        return state.getDestroySpeed(client.level, pos) != -1.0f;
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

    private record Edge(BlockPos pos, double cost) {
    }
}