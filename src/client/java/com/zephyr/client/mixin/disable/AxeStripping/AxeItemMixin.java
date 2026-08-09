package com.zephyr.client.mixin.disable.AxeStripping;

import com.zephyr.client.module.disable.disableAxeStripping;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public class AxeItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void disableStripping(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!disableAxeStripping.INSTANCE.isEnabled()) {return;}
        cir.setReturnValue(InteractionResult.PASS);
    }
}
