package com.zephyr.client.module.combat.KillAura;

import net.minecraft.world.entity.LivingEntity;

public final class TargetManager {

    private TargetManager() {}

    private static LivingEntity target;

    public static LivingEntity getTarget() {
        if (target != null && !target.isAlive()) {
            target = null;
        }
        return target;
    }

    public static void setTarget(LivingEntity living) {
        target = living;
    }

    public static void clear() {
        target = null;
    }
}