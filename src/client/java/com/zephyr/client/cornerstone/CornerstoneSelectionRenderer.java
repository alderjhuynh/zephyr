package com.zephyr.client.cornerstone;

import java.util.Optional;

import com.zephyr.client.render.gizmos.GizmoStyle;
import com.zephyr.client.render.gizmos.Gizmos;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

// Backport of 26.3's SelectionRenderer. 1.21.1 has no LevelRenderEvents /
// net.minecraft.gizmos; gizmos are queued during tick via Zephyr's own
// render.gizmos.Gizmos and drawn at WorldRenderEvents.END (see ZephyrClient).
public final class CornerstoneSelectionRenderer {
    private static final GizmoStyle CORNER_1 = GizmoStyle.stroke(0xFF00E600);
    private static final GizmoStyle CORNER_2 = GizmoStyle.stroke(0xFF00BFFF);
    private static final GizmoStyle AREA = GizmoStyle.stroke(0xFFFFFFFF);

    private CornerstoneSelectionRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> render());
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

        try (var ignored = Gizmos.collect()) {
            first.ifPresent(pos -> Gizmos.cuboid(pos, CORNER_1).setAlwaysOnTop());
            second.ifPresent(pos -> Gizmos.cuboid(pos, CORNER_2).setAlwaysOnTop());

            if (first.isPresent() && second.isPresent()) {
                AABB box = AABB.encapsulatingFullBlocks(first.get(), second.get());
                Gizmos.cuboid(box, AREA);
            }
        }
    }
}
