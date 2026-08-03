package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public final class PotionSaver extends Module {
    public static final PotionSaver INSTANCE = new PotionSaver();
    private PotionSaver() {
        super("Potion Saver", "Attempts to extend the duration of potions", Category.QOL);
    }

    private static final List<MobEffect> effects = List.of(
            MobEffects.STRENGTH.value(),
            MobEffects.ABSORPTION.value(),
            MobEffects.RESISTANCE.value(),
            MobEffects.FIRE_RESISTANCE.value(),
            MobEffects.SPEED.value(),
            MobEffects.HASTE.value(),
            MobEffects.REGENERATION.value(),
            MobEffects.WATER_BREATHING.value(),
            MobEffects.SATURATION.value(),
            MobEffects.LUCK.value(),
            MobEffects.DOLPHINS_GRACE.value(),
            MobEffects.CONDUIT_POWER.value(),
            MobEffects.HERO_OF_THE_VILLAGE.value()
    );

    public boolean shouldFreeze(MobEffect effect) {
        Minecraft client = Minecraft.getInstance();

        return isEnabled()
                && client.player != null
                && effects.contains(effect);
    }
}
