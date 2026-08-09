package com.zephyr.client.module.qol.jade.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Describes a targeted entity, mirroring Jade's {@code EntityAccessor}.
 */
public class EntityAccessor extends Accessor {
    private final Level level;
    private final Entity entity;
    private final EntityHitResult hitResult;

    public EntityAccessor(Level level, Entity entity, EntityHitResult hitResult) {
        this.level = level;
        this.entity = entity;
        this.hitResult = hitResult;
    }

    @Override
    public boolean isBlock() {
        return false;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public Vec3 getHitLocation() {
        return hitResult.getLocation();
    }

    public Entity getEntity() {
        return entity;
    }

    public EntityHitResult getHitResult() {
        return hitResult;
    }
}
