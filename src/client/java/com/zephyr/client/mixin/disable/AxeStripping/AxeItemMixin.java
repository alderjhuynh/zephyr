package com.zephyr.client.mixin.disable.AxeStripping;

import com.zephyr.client.module.disable.disableAxeStripping;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link Item} that backs the
 * {@code disableAxeStripping} module.
 *
 * <p>Since 26.3 axe behavior moved from {@code AxeItem} into a data-driven
 * block-transformer component on plain items, this cancels {@code useOn} only
 * for stacks in the axes tag.
 */
@Mixin(Item.class)
public class AxeItemMixin {

    /**
     * Cancels the axe's {@code useOn} interaction at the head of the call so that
     * logs and wood blocks cannot be stripped when the module is enabled, returning
     * {@link InteractionResult#PASS} instead.
     *
     * @param context the item-use context for the interaction
     * @param cir     the cancellable return-value callback
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void disableStripping(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!disableAxeStripping.INSTANCE.isEnabled()) {return;}
        if (context.getItemInHand().is(ItemTags.AXES)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
