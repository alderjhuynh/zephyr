package com.zephyr.client.mixin;

import com.zephyr.client.module.PickBeforePlace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class BlockPlaceMixin {

    @Shadow @Final private MinecraftClient client;

    @Unique
    private boolean isPicking = false;

    @Inject(
            method = "interactBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInteractBlock(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (isPicking) return;
        if (!PickBeforePlace.enabled) return;

        if (hand != Hand.MAIN_HAND) return;

        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }

        isPicking = true;

        ((BlockPickInvoker) client).invokeDoItemPick();

        ActionResult result = ((ClientPlayerInteractionManager)(Object)this)
                .interactBlock(player, hand, hitResult);

        isPicking = false;

        cir.setReturnValue(result);
    }
}