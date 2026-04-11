package com.zephyr.client.mixin;

import com.zephyr.client.module.ElytraBoost;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public class FireworkRocketItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockRocketPlacement(ItemUsageContext context,
                                            CallbackInfoReturnable<ActionResult> cir) {
        if (ElytraBoost.isBlockingRockets(context.getStack())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
