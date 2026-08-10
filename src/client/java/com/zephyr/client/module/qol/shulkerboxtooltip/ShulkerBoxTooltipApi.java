package com.zephyr.client.module.qol.shulkerboxtooltip;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Static entry point for the ShulkerBoxTooltip module. Unlike the original mod, there are no
 * preview keybinds to poll: a preview is requested whenever the module is enabled.
 */
public final class ShulkerBoxTooltipApi {
    private ShulkerBoxTooltipApi() {
    }

    @Nullable
    public static PreviewProvider getPreviewProviderForStack(ItemStack stack) {
        return PreviewProviderRegistry.getInstance().get(stack);
    }

    /**
     * Returns the provider if the module is enabled and a preview is available for the context.
     */
    @Nullable
    public static PreviewProvider getProviderIfPreviewAvailable(PreviewContext context) {
        if (!ShulkerBoxTooltip.INSTANCE.isEnabled())
            return null;
        PreviewProvider provider = getPreviewProviderForStack(context.stack());

        if (provider != null && provider.shouldDisplay(context) && getCurrentPreviewType(
                provider.isFullPreviewAvailable(context)) != PreviewType.NO_PREVIEW) {
            return provider;
        }
        return null;
    }

    /**
     * Returns the currently requested preview type. Since Zephyr does not poll preview keybinds,
     * this only depends on the module's preview mode setting.
     */
    @NotNull
    public static PreviewType getCurrentPreviewType(boolean hasFullPreviewMode) {
        ShulkerBoxTooltip module = ShulkerBoxTooltip.INSTANCE;

        if (!module.isEnabled())
            return PreviewType.NO_PREVIEW;
        return switch (module.previewMode()) {
            case FULL -> hasFullPreviewMode ? PreviewType.FULL : PreviewType.COMPACT;
            case COMPACT -> PreviewType.COMPACT;
        };
    }

    /**
     * Adds the provider's tooltip lines (e.g. the item count) to the stack tooltip.
     */
    public static void modifyStackTooltip(ItemStack stack, Consumer<Collection<Component>> tooltip) {
        ShulkerBoxTooltip module = ShulkerBoxTooltip.INSTANCE;

        if (!module.isEnabled())
            return;

        Minecraft client = Minecraft.getInstance();
        PreviewContext context = PreviewContext.builder(stack).withOwner(client.player).build();
        PreviewProvider provider = getPreviewProviderForStack(stack);

        if (provider == null)
            return;
        if (provider.showTooltipHints(context)) {
            if (module.tooltipType() == TooltipType.MOD) {
                tooltip.accept(provider.addTooltip(context));
            }
        }
    }
}
