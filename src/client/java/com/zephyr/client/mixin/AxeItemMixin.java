package com.zephyr.client.mixin;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.zephyr.client.disable.disableAxeStripping;

@Mixin(AxeItem.class)
public class AxeItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void disableStripping(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!disableAxeStripping.enabled) {return;}
        cir.setReturnValue(ActionResult.PASS);
    }
}