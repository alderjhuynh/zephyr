package com.zephyr.client.mixin.movement.TridentBoost;

import com.zephyr.client.module.movement.TridentBoost;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void zephyr$allowDryRiptideUse(Level level, Player player, InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (TridentBoost.canUseOutsideWater(player, player.getItemInHand(hand))) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void zephyr$handleDryRiptideRelease(ItemStack stack, Level level, LivingEntity user,
                                                int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (TridentBoost.handleDryRiptide(level, user, stack, remainingUseTicks)) {
            cir.setReturnValue(true);
        }
    }
}
