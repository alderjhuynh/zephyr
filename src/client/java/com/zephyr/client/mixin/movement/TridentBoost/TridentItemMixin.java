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

/**
 * Mixin targeting {@link net.minecraft.world.item.TridentItem}. Injects at the
 * head of {@code use()} to allow starting a Riptide charge on dry land, and at
 * the head of {@code releaseUsing()} to perform the Riptide launch and sound
 * when released outside water. Both injections delegate to the Trident Boost
 * module, enabling dry riptide boosts.
 */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    /** Starts charging the trident on use when a dry Riptide boost is permitted. */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void zephyr$allowDryRiptideUse(Level level, Player player, InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (TridentBoost.canUseOutsideWater(player, player.getItemInHand(hand))) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /** Executes the dry Riptide launch on release and consumes the release when handled. */
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void zephyr$handleDryRiptideRelease(ItemStack stack, Level level, LivingEntity user,
                                                int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (TridentBoost.handleDryRiptide(level, user, stack, remainingUseTicks)) {
            cir.setReturnValue(true);
        }
    }
}
