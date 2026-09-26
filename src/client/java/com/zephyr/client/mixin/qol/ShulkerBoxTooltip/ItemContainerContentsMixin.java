package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import com.zephyr.client.module.qol.shulkerboxtooltip.TooltipType;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin into {@link ItemContainerContents} for the Zephyr ShulkerBoxTooltip
 * module.
 *
 * <p>Cancels the vanilla container contents tooltip line ("Contains N items")
 * when the module is enabled and uses the custom tooltip window, which draws
 * its own count line.
 */
@Mixin(ItemContainerContents.class)
public class ItemContainerContentsMixin {
    /**
     * Suppresses the vanilla "Contains N items" tooltip line when the
     * ShulkerBoxTooltip module renders its custom tooltip.
     *
     * @param tooltipContext     the tooltip context
     * @param consumer           the component consumer for the tooltip lines
     * @param tooltipFlag        the tooltip flag
     * @param dataComponentGetter the component getter for the contents
     * @param ci                 mixin callback used to cancel the line
     */
    @Inject(at = @At("HEAD"), method = "addToTooltip("
            + "Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;"
            + "Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V",
            cancellable = true)
    void zephyr$cancelVanillaContentsTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer,
            TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter, CallbackInfo ci) {
        ShulkerBoxTooltip module = ShulkerBoxTooltip.INSTANCE;

        if (module.isEnabled() && module.tooltipType() == TooltipType.MOD) {
            ci.cancel();
        }
    }
}
