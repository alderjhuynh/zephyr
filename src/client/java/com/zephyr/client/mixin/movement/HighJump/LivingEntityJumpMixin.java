package com.zephyr.client.mixin.movement.HighJump;

import com.zephyr.client.module.movement.HighJump;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @ModifyVariable(
            method = "jumpFromGround",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private float modifyJumpVelocity(float original) {
        return HighJump.modifyJumpVelocity(original);
    }
}
