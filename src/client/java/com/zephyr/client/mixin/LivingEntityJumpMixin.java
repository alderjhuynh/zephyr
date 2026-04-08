package com.zephyr.client.mixin;

import com.zephyr.client.HighJump;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @ModifyVariable(
            method = "jump",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private float modifyJumpVelocity(float original) {
        return HighJump.modifyJumpVelocity(original);
    }
}