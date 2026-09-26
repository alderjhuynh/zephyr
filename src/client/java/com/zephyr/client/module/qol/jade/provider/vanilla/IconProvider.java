package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.Element;
import com.zephyr.client.module.qol.jade.render.IconElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies the left-side icon: the block's item for blocks, the entity's pick
 * result (spawn egg / item) for entities. Empty stacks opt out.
 */
public class IconProvider implements IComponentProvider {
    public static final IconProvider INSTANCE = new IconProvider();

    /**
     * Supplies the left-side icon for the target, keeping the first non-null
     * icon supplied by an earlier provider.
     *
     * @param accessor    the current target
     * @param currentIcon the icon set by a previous provider, or {@code null}
     * @return the target's block item / entity pick result icon, or {@code null}
     */
    @Override
    @Nullable
    public Element getIcon(Accessor accessor, @Nullable Element currentIcon) {
        if (currentIcon != null) {
            return currentIcon;
        }
        if (accessor instanceof BlockAccessor block) {
            Block blockType = block.getBlock();
            if (blockType.asItem() != Items.AIR) {
                return new IconElement(new ItemStack(blockType));
            }
            return null;
        }
        if (accessor instanceof EntityAccessor entity) {
            ItemStack pick = entity.getEntity().getPickResult();
            if (pick != null && !pick.isEmpty()) {
                return new IconElement(pick);
            }
        }
        return null;
    }

    /** Icon-only provider; contributes no tooltip rows. */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
    }
}
