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

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(at = @At("HEAD"), method = "getTooltipImage()Ljava/util/Optional;", cancellable = true)
    private void zephyr$onGetTooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        Player owner = Minecraft.getInstance().player;
        PreviewContext context = PreviewContext.builder((ItemStack) (Object) this).withOwner(owner).build();

        PreviewProvider provider = ShulkerBoxTooltipApi.getProviderIfPreviewAvailable(context);
        if (provider != null)
            cir.setReturnValue(Optional.of(new PreviewTooltipComponent(provider, context)));
    }

    @Inject(at = @At("RETURN"), method =
            "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;")
    private void zephyr$onGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag type,
                                          CallbackInfoReturnable<List<Component>> cir) {
        var tooltip = cir.getReturnValue();
        ShulkerBoxTooltipApi.modifyStackTooltip((ItemStack) (Object) this, tooltip::addAll);
    }

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
