package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewConfiguration;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewMode;
import com.zephyr.client.module.qol.shulkerboxtooltip.TooltipType;

/**
 * Port of ShulkerBoxTooltip. While enabled, hovering over a shulker box (or any other
 * supported container item) shows its contents in the item tooltip.
 *
 * <p>Unlike the original mod there are no preview keybinds to poll: the preview is simply
 * always shown while this module is active. Hooking lives in
 * {@code com.zephyr.client.mixin.qol.ShulkerBoxTooltip.*} and the rendering code in
 * {@code com.zephyr.client.module.qol.shulkerboxtooltip}.
 */
public final class ShulkerBoxTooltip extends Module {
    public static final ShulkerBoxTooltip INSTANCE = new ShulkerBoxTooltip();

    private final EnumSetting<TooltipType> tooltipType = new EnumSetting<>("Tooltip Type", TooltipType.MOD);
    private final EnumSetting<PreviewMode> previewMode = new EnumSetting<>("Preview Mode", PreviewMode.FULL);
    private final NumberSetting maxRowSize = new NumberSetting("Max Row Size", 9.0, 1.0, 9.0, 1.0);
    private final BooleanSetting shortItemCounts = new BooleanSetting("Short Item Counts", false);
    private final BooleanSetting useBoxColors = new BooleanSetting("Use Box Colors", true);
    private final BooleanSetting hideShulkerBoxLore = new BooleanSetting("Hide Shulker Box Lore", false);

    private ShulkerBoxTooltip() {
        super("ShulkerBoxTooltip",
                "Shows the contents of shulker boxes and other containers in their tooltip",
                Category.QOL);
        addSetting(tooltipType);
        addSetting(previewMode);
        addSetting(maxRowSize);
        addSetting(shortItemCounts);
        addSetting(useBoxColors);
        addSetting(hideShulkerBoxLore);
    }

    /** Read-only snapshot of the settings used by the preview renderers. */
    public PreviewConfiguration configuration() {
        return new PreviewConfiguration((int) (double) maxRowSize.get(), true, shortItemCounts.get(), useBoxColors.get());
    }

    /** Whether the mod-style (custom window) or vanilla-style (bundle grid) tooltip is used. */
    public TooltipType tooltipType() {
        return tooltipType.get();
    }

    /** Whether the full slot grid or the compact merged view is shown. */
    public PreviewMode previewMode() {
        return previewMode.get();
    }

    /** Whether shulker box lore text is hidden from the tooltip. */
    public boolean hideLore() {
        return hideShulkerBoxLore.get();
    }
}
