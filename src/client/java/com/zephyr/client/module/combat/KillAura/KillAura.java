package com.zephyr.client.module.combat.KillAura;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.combat.Reach;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Automatically attacks nearby entities. In AURA mode it attacks the closest eligible
 * entity every tick; in ASSIST mode it attacks whatever entity the player last looked at and
 * attacked, up to a scaled distance. Respects the "Players Only" filter and the {@code Reach}
 * module for the effective attack range.
 */
public final class KillAura extends Module {
    public static final KillAura INSTANCE = new KillAura();
    private KillAura() {
        super("KillAura", "Automatically attacks for you", Category.COMBAT);
        addSetting(mode);
        addSetting(playersOnly);
        addSetting(distanceMult);
    }

    /** Scales the maximum distance at which an ASSIST-mode target is still attacked. */
    private final NumberSetting distanceMult = new NumberSetting("Discard Target Distance Multiplier", 3, 1, 3, 1);
    private final EnumSetting<KillAura.Mode> mode = new EnumSetting<>("Mode", KillAura.Mode.AURA);
    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", false);

    /** How KillAura picks its attack target. */
    public enum Mode {
        /** Attack the closest eligible entity automatically. */
        AURA,
        /** Attack only the entity the player is looking at. */
        ASSIST
    }


    /** Attacks according to the selected mode each tick while the module is enabled. */
    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null || client.level == null) return;
        if (mode.get() == Mode.AURA) {
            tickAura(client);
        } else {
            tickAssist(client);
        }
    }

    private void tickAura(Minecraft client) {
        LivingEntity target = findClosestTarget(client);
        if (target != null) {
            attack(client, target);
        }
    }

    private void tickAssist(Minecraft client) {
        LivingEntity target = TargetManager.getTarget();
        if (target == null) return;

        double reach = getEntityReach(client);
        double maxDistance = reach * distanceMult.get();
        double maxDistanceSq = maxDistance * maxDistance;

        if (client.player.distanceToSqr(target) > maxDistanceSq) {
            TargetManager.clear();
            return;
        }

        attack(client, target);
    }

    private static void attack(Minecraft client, LivingEntity target) {
        LocalPlayer player = client.player;
        if (player.getAttackStrengthScale(0.5F) < 0.98F) return;
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private LivingEntity findClosestTarget(Minecraft client) {
        double reach = getEntityReach(client);
        double reachSq = reach * reach;
        LocalPlayer player = client.player;

        LivingEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == player || !living.isAlive()) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;

            double distanceSq = player.distanceToSqr(living);
            if (distanceSq > reachSq) continue;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = living;
            }
        }
        return best;
    }

    /**
     * Returns the effective entity interaction range, combining the player's base attribute
     * value with the {@code Reach} module's entity reach bonus when that module is enabled.
     */
    public Double getEntityReach(Minecraft client) {
        Double base = client.player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            Double add = Reach.INSTANCE.entityReach.get();
            return add + base;
        }
        return base;
    }
}
