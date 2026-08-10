package com.zephyr.client.mixin.qol.PickBeforePlace;

import com.zephyr.client.module.qol.PickBeforePlace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class BlockPlaceMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique
    private boolean isPicking = false;

    @Inject(
            method = "useItemOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (isPicking) return;
        if (!PickBeforePlace.INSTANCE.isEnabled()) return;

        if (hand != InteractionHand.MAIN_HAND) return;

        if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        isPicking = true;

        ((BlockPickInvoker) minecraft).invokePickBlock();

        InteractionResult result = ((MultiPlayerGameMode)(Object)this)
                .useItemOn(player, hand, hitResult);

        isPicking = false;

        cir.setReturnValue(result);
    }
}