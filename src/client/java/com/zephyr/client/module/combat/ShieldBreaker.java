package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class ShieldBreaker extends Module {
    public static final ShieldBreaker INSTANCE = new ShieldBreaker();
    private ShieldBreaker() {
        super("Shieldbreaker", "Automatically breaks shields", Category.COMBAT);
    }

    public static boolean isTargetBlockingWithShield(RemotePlayer target, LocalPlayer attacker) {
        if (target == null || attacker == null) return false;

        if (!target.isUsingItem()) return false;
        if (!target.getActiveItem().is(Items.SHIELD)) return false;

        Vec3 targetLook = target.getLookAngle();
        Vec3 directionToAttacker = attacker.getPosition(1.0f).subtract(target.getPosition(1.0f));

        double dot = targetLook.dot(directionToAttacker);

        return dot > 0.3;
    }

    public static RemotePlayer getCrosshairPlayer() {
        Minecraft client = Minecraft.getInstance();

        if (client.crosshairPickEntity instanceof RemotePlayer player) {
            return player;
        }

        return null;
    }
}
