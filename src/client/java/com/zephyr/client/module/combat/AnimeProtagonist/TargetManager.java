package com.zephyr.client.module.combat.AnimeProtagonist;

import net.minecraft.world.entity.LivingEntity;

/**
 * Static store for the entity most recently attacked by the local player, populated by the
 * AnimeProtagonist attack mixin and consumed by {@link AnimeProtagonist} to decide where to
 * teleport.
 */
public final class TargetManager {

    private TargetManager() {}

    private static LivingEntity target;

    /** Returns the stored target, clearing it first if it is no longer alive. */
    public static LivingEntity getTarget() {
        if (target != null && !target.isAlive()) {
            target = null;
        }
        return target;
    }

    /** Stores the given living entity as the current target. */
    public static void setTarget(LivingEntity living) {
        target = living;
    }

    /** Clears the stored target. */
    public static void clear() {
        target = null;
    }
}