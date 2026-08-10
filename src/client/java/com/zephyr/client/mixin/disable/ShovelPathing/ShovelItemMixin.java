package com.zephyr.client.mixin.disable.ShovelPathing;

import com.zephyr.client.module.disable.disableShovelPathing;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link ShovelItem} that backs the {@code disableShovelPathing}
 * module.
 */
@Mixin(ShovelItem.class)
public class ShovelItemMixin {

    /**
     * Cancels the shovel's {@code useOn} interaction at the head of the call so that
     * grass and dirt cannot be turned into path blocks when the module is enabled,
     * returning {@link InteractionResult#PASS} instead.
     *
     * @param context the item-use context for the interaction
     * @param cir     the cancellable return-value callback
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void disablePathing(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!disableShovelPathing.INSTANCE.isEnabled()) return;
        cir.setReturnValue(InteractionResult.PASS);
    }
}
