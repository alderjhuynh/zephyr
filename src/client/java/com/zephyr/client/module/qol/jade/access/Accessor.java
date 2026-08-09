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
    public abstract boolean isBlock();

    public boolean isEntity() {
        return !isBlock();
    }

    public abstract Level getLevel();

    public abstract Vec3 getHitLocation();
}
