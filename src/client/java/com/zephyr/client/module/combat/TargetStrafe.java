package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Combat module that automatically circles the nearest eligible target while the player is
 * moving. When a target is in range and the player gives a movement input, the TargetStrafe
 * KeyboardInput mixin turns the player to face the target and redirects the input into a
 * left/right strafe, so the player keeps orbiting the target instead of running straight at
 * it. The orbit direction, player-only filter and whether strafing is limited to the attack
 * key are all configurable.
 */
public final class TargetStrafe extends Module {
    public static final TargetStrafe INSTANCE = new TargetStrafe();

    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", true);
    private final BooleanSetting onlyWhileAttacking = new BooleanSetting("Only While Attacking", false);
    private final EnumSetting<OrbitDirection> direction = new EnumSetting<>("Orbit Direction", OrbitDirection.LEFT);

    private TargetStrafe() {
        super("Target Strafe", "Orbits around a nearby target while you move", Category.COMBAT);
        addSetting(playersOnly);
        addSetting(onlyWhileAttacking);
        addSetting(direction);
    }

    /** Which way around the target the player circles. */
    public enum OrbitDirection {
        LEFT,
        RIGHT
    }

    /** Whether the strafe is limited to nearby players instead of all living entities. */
    public boolean playersOnly() {
        return playersOnly.get();
    }

    /** Whether strafing only happens while the attack key is held. */
    public boolean onlyWhileAttacking() {
        return onlyWhileAttacking.get();
    }

    /** Whether the player orbits counter-clockwise (left) around the target. */
    public boolean orbitLeft() {
        return direction.get() == OrbitDirection.LEFT;
    }

    /** Returns the closest eligible entity to orbit, or null if none is in range. */
    public LivingEntity findTarget(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return null;
        LocalPlayer player = client.player;

        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            reach += Reach.INSTANCE.entityReach.get();
        }
        double reachSq = reach * reach;

        LivingEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == player || !living.isAlive() || living.isSpectator()) continue;
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
}
