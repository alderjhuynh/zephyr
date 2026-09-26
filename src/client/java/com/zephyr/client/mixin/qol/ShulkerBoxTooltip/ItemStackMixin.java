package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewContext;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewProvider;
import com.zephyr.client.module.qol.shulkerboxtooltip.ShulkerBoxTooltipApi;
import com.zephyr.client.module.qol.shulkerboxtooltip.tooltip.PreviewTooltipComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Mixin into {@link ItemStack} wiring the Zephyr ShulkerBoxTooltip module into
 * the item tooltip pipeline.
 *
 * <p>Injects a preview tooltip component when a supported container item is
 * hovered, appends additional tooltip lines via the
 * {@link ShulkerBoxTooltipApi} entrypoints, and hides the lore line of shulker
 * box items when the "Hide Shulker Box Lore" setting is enabled.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
    /**
     * Replaces the stack's tooltip image with a {@link PreviewTooltipComponent}
     * when a preview provider is available for the hovered container item.
     *
     * @param cir mixin callback used to substitute the tooltip image
     */
    @Inject(at = @At("HEAD"), method = "getTooltipImage()Ljava/util/Optional;", cancellable = true)
    private void zephyr$onGetTooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        Player owner = Minecraft.getInstance().player;
        PreviewContext context = PreviewContext.builder((ItemStack) (Object) this).withOwner(owner).build();

        PreviewProvider provider = ShulkerBoxTooltipApi.getProviderIfPreviewAvailable(context);
        if (provider != null)
            cir.setReturnValue(Optional.of(new PreviewTooltipComponent(provider, context)));
    }

    /**
     * Appends module tooltip lines (registered via
     * {@link ShulkerBoxTooltipApi#modifyStackTooltip}) to the stack's computed
     * tooltip.
     *
     * @param context the item tooltip context
     * @param player  the player viewing the tooltip
     * @param type    the tooltip flag
     * @param cir     mixin callback providing the computed tooltip lines
     */
    @Inject(at = @At("RETURN"), method =
            "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;")
    private void zephyr$onGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag type,
                                          CallbackInfoReturnable<List<Component>> cir) {
        var tooltip = cir.getReturnValue();
        ShulkerBoxTooltipApi.modifyStackTooltip((ItemStack) (Object) this, tooltip::addAll);
    }

    /**
     * Hides the lore tooltip line on shulker box items while the module's
     * "Hide Shulker Box Lore" setting is enabled.
     *
     * @param componentType    the data component being added to the tooltip
     * @param tooltipContext   the tooltip context
     * @param tooltipDisplay   the tooltip display settings
     * @param consumer         the component consumer for the tooltip lines
     * @param tooltipFlag      the tooltip flag
     * @param ci               mixin callback used to cancel the lore line
     */
    @Inject(at = @At("HEAD"), method = "addToTooltip("
            + "Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;"
            + "Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            cancellable = true)
    private <T extends TooltipProvider> void zephyr$hideShulkerBoxLore(DataComponentType<T> componentType,
            Item.TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> consumer,
            TooltipFlag tooltipFlag, CallbackInfo ci) {
        if (componentType == DataComponents.LORE) {
            ShulkerBoxTooltip module = ShulkerBoxTooltip.INSTANCE;

            if (module.isEnabled() && module.hideLore()) {
                Item item = ((ItemStack) (Object) this).getItem();

                if (item instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock) {
                    ci.cancel();
                }
            }
        }
    }
}
