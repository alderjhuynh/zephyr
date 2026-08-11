package com.zephyr.client.mixin.movement.Jesus;

import com.zephyr.client.module.movement.Jesus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link LivingEntity} backing the {@code Jesus} module. While the module is
 * enabled, the local player is reported as able to stand on water and is given a solid
 * collision shape on top of water blocks, so the water surface acts like ground. The effect
 * is skipped while sneaking, swimming or flying so the player can still dive and swim.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** Whether the local player should currently walk on water surfaces. */
    private static boolean zephyr$shouldWalkOnWater(Object self) {
        if (!Jesus.INSTANCE.isEnabled()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player != self) return false;

        LocalPlayer player = client.player;
        return !player.isShiftKeyDown() && !player.isSwimming() && !player.getAbilities().flying;
    }

    /**
     * Treats water as a standable fluid for the local player while Jesus is enabled.
     *
     * @param fluidState the fluid state being checked
     * @param cir        mixin callback used to override the result
     */
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void zephyr$walkOnWater(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (!fluidState.is(FluidTags.WATER)) return;
        if (zephyr$shouldWalkOnWater(this)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Gives the player a solid block-shaped surface on top of water so it can be walked on.
     *
     * @param cir mixin callback used to override the shape
     */
    @Inject(method = "getLiquidCollisionShape", at = @At("HEAD"), cancellable = true)
    private void zephyr$solidWaterSurface(CallbackInfoReturnable<VoxelShape> cir) {
        if (zephyr$shouldWalkOnWater(this)) {
            cir.setReturnValue(Shapes.block());
        }
    }
}
