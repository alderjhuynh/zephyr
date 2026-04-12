package com.zephyr.client.mixin;

import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.zephyr.client.disable.disableShovelPathing;

@Mixin(ShovelItem.class)
public class ShovelItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void disablePathing(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!disableShovelPathing.enabled) return;
        cir.setReturnValue(ActionResult.PASS);
    }
}