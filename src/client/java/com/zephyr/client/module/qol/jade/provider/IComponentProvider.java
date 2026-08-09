package com.zephyr.client.module.qol.jade.provider;

import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.render.Element;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import org.jetbrains.annotations.Nullable;

/**
 * A source of overlay content, mirroring Jade's {@code IBlockComponentProvider} /
 * {@code IEntityComponentProvider}. Providers are chained in registration order;
 * each one appends rows to the {@link Tooltip}. {@link #getIcon(Accessor, Element)}
 * lets icon-capable providers set (or override) the tooltip's left-side icon.
 */
public interface IComponentProvider {
    /** The most recent element, or {@code null} if no provider set one yet. */
    @Nullable
    default Element getIcon(Accessor accessor, @Nullable Element currentIcon) {
        return currentIcon;
    }

    /** Appends one or more rows to the tooltip for the given target. */
    void appendTooltip(Tooltip tooltip, Accessor accessor);
}
