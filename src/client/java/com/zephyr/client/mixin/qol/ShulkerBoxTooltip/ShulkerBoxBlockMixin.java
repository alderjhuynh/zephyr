package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import com.zephyr.client.module.qol.shulkerboxtooltip.TooltipType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Cancels the vanilla shulker box contents tooltip lines ("1 Diamond x5", "and N more...") when
 * the module uses the custom tooltip window, which draws its own contents instead. In 1.21.1
 * these lines are produced by {@code ShulkerBoxBlock.appendHoverText} (there is no
 * {@code ItemContainerContents.addToTooltip} yet).
 */
@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    @Inject(at = @At("HEAD"), method = "appendHoverText("
            + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;"
            + "Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
            cancellable = true)
    void zephyr$cancelVanillaContentsTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines,
            TooltipFlag flag, CallbackInfo ci) {
        ShulkerBoxTooltip module = ShulkerBoxTooltip.INSTANCE;

        if (module.isEnabled() && module.tooltipType() == TooltipType.MOD) {
            ci.cancel();
        }
    }
}
