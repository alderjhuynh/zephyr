package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;

import java.util.Locale;

/**
 * Horse-family stats, mirroring Jade's {@code HorseStatsProvider}: jump height and
 * movement speed derived from the client-synced base attributes, plus llama carry
 * strength. Camels are skipped (no relevant stats).
 */
public class HorseStatsProvider implements IComponentProvider {
    public static final HorseStatsProvider INSTANCE = new HorseStatsProvider();

    private static final double MAX_JUMP_STRENGTH = 1.0;
    private static final double MAX_MOVEMENT_SPEED = 0.3375;
    private static final double MAX_JUMP_HEIGHT = getJumpHeight(MAX_JUMP_STRENGTH);
    private static final double MAX_SPEED = getSpeed(MAX_MOVEMENT_SPEED);

    /**
     * Appends jump height, movement speed, and/or llama strength rows for the
     * targeted equine when the corresponding module setting is enabled.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showHorseStats() || !(accessor instanceof EntityAccessor entity)) {
            return;
        }
        if (!(entity.getEntity() instanceof AbstractHorse horse)) {
            return;
        }

        if (horse instanceof Llama llama) {
            tooltip.add(new TextElement(Component.literal("Strength: " + llama.getStrength() + "/5"),
                    JadeColors.NORMAL, true));
            return;
        }
        if (horse instanceof Camel) {
            return;
        }

        if (horse.getAttributes().hasAttribute(Attributes.JUMP_STRENGTH)) {
            double jump = getJumpHeight(horse.getAttributeBaseValue(Attributes.JUMP_STRENGTH));
            tooltip.add(new TextElement(Component.literal("Jump: " + format(jump) + " (" + Math.round(jump / MAX_JUMP_HEIGHT * 100) + "%)"),
                    JadeColors.NORMAL, true));
        }
        if (horse.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
            double speed = getSpeed(horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
            tooltip.add(new TextElement(Component.literal("Speed: " + format(speed) + " (" + Math.round(speed / MAX_SPEED * 100) + "%)"),
                    JadeColors.NORMAL, true));
        }
    }

    private static double getJumpHeight(double jumpStrength) {
        return 4.53680079 * jumpStrength * jumpStrength + 1.61431730 * jumpStrength - 0.22656224;
    }

    private static double getSpeed(double speed) {
        // https://minecraft.wiki/w/Horse#Movement_speed
        return speed * 43.171815466666658 - 0.000000339999999;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
