package com.zephyr.client.module.qol.jade.ray;

import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Port of Jade's {@code RayTracing}. Fires a ray from the camera entity's eye
 * along its view vector, considering both entities and blocks, and prefers
 * whichever is closest. Reuses the vanilla crosshair result when it already points
 * at a valid entity, and honors the module's extended-reach setting.
 */
public final class RayTracer {
    private RayTracer() {
    }

    public static Accessor raycast(Minecraft mc, double extendedReach) {
        Entity viewEntity = mc.getCameraEntity();
        Player viewPlayer = viewEntity instanceof Player player ? player : mc.player;
        if (viewEntity == null || viewPlayer == null || mc.level == null) {
            return null;
        }

        HitResult vanilla = mc.hitResult;
        if (vanilla != null && vanilla.getType() == Type.ENTITY) {
            Entity target = ((EntityHitResult) vanilla).getEntity();
            if (canBeTarget(target, viewEntity, mc)) {
                return new EntityAccessor(mc.level, target, (EntityHitResult) vanilla);
            }
        }

        double blockReach = viewPlayer.blockInteractionRange() + extendedReach;
        double entityReach = viewPlayer.entityInteractionRange() + extendedReach;
        HitResult target = rayTrace(viewEntity, blockReach, entityReach, mc);
        if (target instanceof BlockHitResult blockHit && blockHit.getType() == Type.BLOCK) {
            return new BlockAccessor(mc.level, blockHit.getBlockPos(), blockHit);
        }
        if (target instanceof EntityHitResult entityHit) {
            return new EntityAccessor(mc.level, entityHit.getEntity(), entityHit);
        }
        return null;
    }

    // from ProjectileUtil, as used by Jade
    private static EntityHitResult getEntityHitResult(
            Level worldIn,
            Entity projectile,
            Vec3 startVec,
            Vec3 endVec,
            AABB boundingBox,
            Predicate<Entity> filter) {
        double d0 = Double.MAX_VALUE;
        Entity entity = null;

        for (Entity entity1 : worldIn.getEntities(projectile, boundingBox, filter)) {
            AABB axisalignedbb = entity1.getBoundingBox();
            if (axisalignedbb.getSize() < 0.3) {
                axisalignedbb = axisalignedbb.inflate(0.3);
            }
            if (axisalignedbb.contains(startVec)) {
                entity = entity1;
                break;
            }
            Optional<Vec3> optional = axisalignedbb.clip(startVec, endVec);
            if (optional.isPresent()) {
                double d1 = startVec.distanceToSqr(optional.get());
                if (d1 < d0) {
                    entity = entity1;
                    d0 = d1;
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity);
    }

    private static HitResult rayTrace(Entity entity, double blockReach, double entityReach, Minecraft mc) {
        Level world = entity.level();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eyePosition = entity.getEyePosition(partialTick);
        Vec3 lookVector = entity.getViewVector(partialTick);
        Vec3 traceStart = eyePosition;
        Vec3 traceEnd = traceStart.add(lookVector.scale(entityReach));

        // when the vanilla pick already hit a block, only search entities closer than it
        if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            Vec3 toHit = mc.hitResult.getLocation().subtract(traceStart);
            if (toHit.lengthSqr() < entityReach * entityReach) {
                traceEnd = traceStart.add(lookVector.scale(toHit.length() + 1e-5));
            }
        }

        AABB bound = new AABB(traceStart, traceEnd);
        Predicate<Entity> predicate = e -> canBeTarget(e, entity, mc);
        EntityHitResult entityResult = getEntityHitResult(world, entity, traceStart, traceEnd, bound, predicate);

        traceEnd = traceStart.add(lookVector.scale(blockReach * 1.001));
        ClipContext context = new ClipContext(traceStart, traceEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                CollisionContext.of(entity));
        BlockHitResult blockResult = world.clip(context);

        if (entityResult != null) {
            if (blockResult.getType() == Type.BLOCK) {
                double entityDist = entityResult.getLocation().distanceToSqr(traceStart);
                double blockDist = blockResult.getLocation().distanceToSqr(traceStart);
                if (entityDist < blockDist) {
                    return entityResult;
                }
            } else {
                return entityResult;
            }
        }
        if (blockResult.getType() == Type.MISS && mc.hitResult instanceof BlockHitResult hit) {
            // weird, we didn't hit a block in our way. try the vanilla result
            blockResult = hit;
        }
        return blockResult.getType() == Type.BLOCK ? blockResult : null;
    }

    private static boolean canBeTarget(Entity target, Entity viewEntity, Minecraft mc) {
        if (target.isRemoved()) {
            return false;
        }
        if (target.isSpectator()) {
            return false;
        }
        if (target == viewEntity.getVehicle()) {
            return false;
        }
        if (target instanceof Projectile projectile && projectile.tickCount <= 10 &&
                !target.level().tickRateManager().isEntityFrozen(target)) {
            return false;
        }
        if (viewEntity instanceof Player player) {
            if (target.isInvisibleTo(player)) {
                return false;
            }
            if (mc.gameMode != null && mc.gameMode.isDestroying() && target.getType() == EntityType.ITEM) {
                return false;
            }
        } else if (target.isInvisible()) {
            return false;
        }
        return true;
    }
}
