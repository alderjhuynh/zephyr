package com.zephyr.client.module.combat.KillAura;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.mixin.combat.KillAura.ForceAttackMixin;
import com.zephyr.client.module.combat.Reach;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class KillAura extends Module {
    public static final KillAura INSTANCE = new KillAura();
    private KillAura() {
        super("KillAura", "Automatically attacks for you", Category.COMBAT);
        addSetting(mode);
        addSetting(distanceMult);
    }

    private final NumberSetting distanceMult = new NumberSetting("Discard Target Distance Multiplier", 3, 1, 3, 1);
    private final EnumSetting<KillAura.Mode> mode = new EnumSetting<>("Mode", KillAura.Mode.AURA);
    public enum Mode {
        AURA,
        ASSIST
    }


    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null) return;
        if (mode.get() != Mode.ASSIST || !isEnabled()) return;
        LivingEntity target = TargetManager.getTarget();
        if (target == null) return;

        if (!target.isAlive()) {TargetManager.clear(); return;}

        Double inRangeDistance = getEntityReach(client);

        double maxDistanceSq = inRangeDistance * KillAura.INSTANCE.distanceMult.get();
        double actualDistanceSq = client.player.distanceToSqr(target);

        if (actualDistanceSq > maxDistanceSq) {
            TargetManager.clear();
            return;
        }
        Vec3 pos = target.getEyePosition();
        smoothLook(client, pos, 0.5f);

        if (client.player.getAttackStrengthScale(0.5F) > 0.98F && getCrosshairEntity() != null) {
            ((ForceAttackMixin) client).invokeDoAttack();

        }
    }

    private static void smoothLook(Minecraft client, Vec3 targetPos, float speed) {
        var player = client.player;
        if (player == null) return;

        Vec3 eyes = player.getEyePosition();
        Vec3 delta = targetPos.subtract(eyes);

        double dx = delta.x;
        double dy = delta.y;
        double dz = delta.z;

        float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(
                dy,
                Math.sqrt(dx * dx + dz * dz)
        )));

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        yaw += wrapDegrees(targetYaw - yaw) * speed;
        pitch += (targetPitch - pitch) * speed;

        player.setYRot(yaw);
        player.setXRot(pitch);

        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    public static Entity getCrosshairEntity() {
        Minecraft client = Minecraft.getInstance();

        if (client.crosshairPickEntity instanceof Entity entity) {
            return entity;
        }

        return null;
    }

    public Double getEntityReach(Minecraft client) {
        Double base = client.player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            Double add = Reach.INSTANCE.entityReach.get();
            return add + base;
        }
        return base;
    }
}
