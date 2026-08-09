package com.zephyr.client.module.qol.shulkerboxtooltip.tooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewContext;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewProvider;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record PreviewTooltipComponent(PreviewProvider provider, PreviewContext context) implements TooltipComponent {
}
