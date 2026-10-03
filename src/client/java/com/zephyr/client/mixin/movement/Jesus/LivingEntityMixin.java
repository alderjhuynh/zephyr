package com.zephyr.client.mixin.movement.Jesus;

import com.zephyr.client.module.movement.Jesus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Backport of 26.3's Jesus mixin. 1.21.1 LivingEntity has canStandOnFluid but
// no getLiquidCollisionShape; returning true here is enough because 1.21.1
// travel() skips the swim branch for standable fluids, letting the player
// walk on the surface with normal ground physics.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private static boolean zephyr$shouldWalkOnWater(Object self) {
        if (!Jesus.INSTANCE.isEnabled()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player != self) return false;

        LocalPlayer player = client.player;
        return !player.isShiftKeyDown() && !player.isSwimming() && !player.getAbilities().flying;
    }

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void zephyr$walkOnWater(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (!fluidState.is(FluidTags.WATER)) return;
        if (zephyr$shouldWalkOnWater(this)) {
            cir.setReturnValue(true);
        }
    }
}
