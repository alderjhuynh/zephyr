package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Attacks whenever a valid entity is in the crosshair and the player's attack cooldown is
 * fully charged. Combines the player's base entity interaction range with the {@code Reach}
 * module's entity reach bonus.
 */
public final class TriggerBot extends Module {
    public static final TriggerBot INSTANCE = new TriggerBot();

    private TriggerBot() {
        super("TriggerBot", "Attacks whenever an entity is in your crosshair and your cooldown is full", Category.COMBAT);
    }

    /** Attacks the crosshair entity each tick once it is in range and the cooldown is ready. */
    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null || client.level == null) return;

        LocalPlayer player = client.player;
        Entity target = client.crosshairPickEntity;
        if (target == null || target == player || !target.isAlive() || target.isSpectator()) return;

        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Reach.INSTANCE.isEnabled()) {
            reach += Reach.INSTANCE.entityReach.get();
        }
        if (player.distanceToSqr(target) > reach * reach) return;

        if (player.getAttackStrengthScale(0.5F) < 1.0F) return;

        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
