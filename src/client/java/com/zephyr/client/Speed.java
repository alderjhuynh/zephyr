package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Speed {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = true;

    public static void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!enabled) return;

        StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.SPEED);
        mc.player.addStatusEffect(
                new StatusEffectInstance(
                        StatusEffects.SPEED,
                        -1,
                        1,
                        false,
                        false,
                        false
                )
        );
    }
}
