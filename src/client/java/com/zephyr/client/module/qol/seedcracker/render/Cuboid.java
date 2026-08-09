package com.zephyr.client.module.qol.seedcracker.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class Cuboid {
    private final AABB box;
    private final int argb;
    private final BlockPos centerPos;

    public Cuboid(AABB box, int argb) {
        this.box = box;
        this.argb = argb;
        this.centerPos = BlockPos.containing(box.getCenter());
    }

    public Cuboid(BoundingBox boundingBox, int argb) {
        this(AABB.of(boundingBox), argb);
    }

    public Cuboid(BlockPos pos, int argb) {
        this(new AABB(pos), argb);
    }

    public Cuboid(BlockPos pos, Vec3i size, int argb) {
        this(AABB.encapsulatingFullBlocks(pos, pos.offset(size)), argb);
    }

    public BlockPos getCenterPos() {
        return this.centerPos;
    }

    public int getArgb() {
        return this.argb;
    }

    public AABB getBox() {
        return this.box;
    }

    /** Draws this cuboid as a stroke-only outline via zephyr's gizmos. */
    public void render() {
        Gizmos.cuboid(this.box, GizmoStyle.stroke(this.argb))
                .setAlwaysOnTop();
    }
}
