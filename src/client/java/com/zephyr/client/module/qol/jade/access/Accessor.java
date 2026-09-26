package com.zephyr.client.module.qol.jade.access;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Abstract description of the thing currently under the crosshair, mirroring
 * Jade's {@code Accessor}. Concrete subclasses wrap a {@code BlockHitResult} or an
 * {@code EntityHitResult}; the providers registry picks the matching provider set
 * based on {@link #isBlock()}.
 */
public abstract class Accessor {
    /** @return whether the target is a block rather than an entity */
    public abstract boolean isBlock();

    /** @return whether the target is an entity rather than a block */
    public boolean isEntity() {
        return !isBlock();
    }

    /** @return the level the target resides in */
    public abstract Level getLevel();

    /** @return the exact hit position in world coordinates */
    public abstract Vec3 getHitLocation();
}
