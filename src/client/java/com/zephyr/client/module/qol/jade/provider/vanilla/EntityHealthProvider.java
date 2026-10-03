package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.ProgressElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.Locale;

/**
 * Health + armor rows for living entities, mirroring Jade's
 * {@code EntityHealthAndArmorProvider}. Everything is read from the client-side
 * entity data watcher (health, max health, armor value), so unlike the original no
 * server round-trip is required for absorption; the bars are drawn with the
 * module's own {@link ProgressElement} instead of heart sprites.
 */
public class EntityHealthProvider implements IComponentProvider {
    public static final EntityHealthProvider INSTANCE = new EntityHealthProvider();

    /**
     * Appends health and/or armor progress bars for the targeted living entity
     * (skipping armor stands), gated by the module's health/armor settings.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!(accessor instanceof EntityAccessor entity) || !(entity.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof ArmorStand) {
            return;
        }

        boolean showHealth = Jade.INSTANCE.showMobHealth();
        boolean showArmor = Jade.INSTANCE.showMobArmor() && living.getArmorValue() > 0;
        if (!showHealth && !showArmor) {
            return;
        }

        if (showHealth) {
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();
            if (maxHealth > 0) {
                Component text = Component.literal(format(health) + " / " + format(maxHealth));
                tooltip.add(new ProgressElement(text, health / maxHealth, JadeColors.HEALTH,
                        JadeColors.BAR_BACKGROUND, JadeColors.NORMAL));
            }
        }

        if (showArmor) {
            int armor = living.getArmorValue();
            tooltip.add(new ProgressElement(Component.literal(armor + " Armor"), armor / 20F,
                    JadeColors.ARMOR, JadeColors.BAR_BACKGROUND, JadeColors.NORMAL));
        }
    }

    private static String format(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
