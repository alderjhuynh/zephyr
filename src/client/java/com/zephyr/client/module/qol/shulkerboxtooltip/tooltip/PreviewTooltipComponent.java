package com.zephyr.client.module.qol.shulkerboxtooltip.tooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewContext;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewProvider;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Tooltip data linking a {@link PreviewProvider} with the {@link PreviewContext}
 * it should render for. Consumed by {@link PreviewClientTooltipComponent} on the
 * client.
 */
public record PreviewTooltipComponent(PreviewProvider provider, PreviewContext context) implements TooltipComponent {
}
