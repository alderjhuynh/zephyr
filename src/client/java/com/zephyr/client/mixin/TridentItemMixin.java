package com.zephyr.client.mixin;

import com.zephyr.client.module.TridentBoost;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void zephyr$allowDryRiptideUse(World world, PlayerEntity user, Hand hand,
                                           CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (!TridentBoost.canUseOutsideWater(user, stack)) {
            return;
        }

        user.setCurrentHand(hand);
        cir.setReturnValue(TypedActionResult.consume(stack));
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void zephyr$handleDryRiptideRelease(ItemStack stack, World world, LivingEntity user, int remainingUseTicks,
                                                CallbackInfo ci) {
        if (TridentBoost.handleDryRiptide(world, user, stack, remainingUseTicks)) {
            ci.cancel();
        }
    }
}
