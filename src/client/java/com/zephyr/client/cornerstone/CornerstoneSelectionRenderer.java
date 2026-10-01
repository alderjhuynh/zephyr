package com.zephyr.client.cornerstone;

import java.util.Optional;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/**
 * Zephyr {@code zcommand} port of cornerstone's {@code SelectionRenderer}.
 *
 * <p>Renders the two selected corners (green = corner 1, blue = corner 2)
 * plus the white encompassing volume while selection mode is on or a save
 * is in progress.
 */
public final class CornerstoneSelectionRenderer {
    private static final GizmoStyle CORNER_1 = GizmoStyle.strokeAndFill(0xFF00E600, 3.0F, 0x6600E600);
    private static final GizmoStyle CORNER_2 = GizmoStyle.strokeAndFill(0xFF00BFFF, 3.0F, 0x6600BFFF);
    private static final GizmoStyle AREA = GizmoStyle.strokeAndFill(0xFFFFFFFF, 2.0F, 0x20FFFFFF);

    private CornerstoneSelectionRenderer() {}

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());
    }

    private static void render() {
        if (!CornerstoneSelection.isActive() && !CornerstoneSaver.isBusy()) {
            return;
        }
        Optional<BlockPos> first = CornerstoneSelection.first();
        Optional<BlockPos> second = CornerstoneSelection.second();
        if (first.isEmpty() && second.isEmpty()) {
            return;
        }

        first.ifPresent(pos -> Gizmos.cuboid(pos, CORNER_1).setAlwaysOnTop());
        second.ifPresent(pos -> Gizmos.cuboid(pos, CORNER_2).setAlwaysOnTop());

        if (first.isPresent() && second.isPresent()) {
            AABB box = AABB.encapsulatingFullBlocks(first.get(), second.get());
            Gizmos.cuboid(box, AREA);
        }
    }
}
