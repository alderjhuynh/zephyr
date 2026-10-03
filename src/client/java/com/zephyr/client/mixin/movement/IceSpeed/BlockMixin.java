package com.zephyr.client.mixin.movement.IceSpeed;

import com.zephyr.client.module.movement.IceSpeed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1.21.1 Block.getFriction() has the same no-arg signature as 26.3.
@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "getFriction", at = @At("RETURN"), cancellable = true)
    private void zephyr$reduceIceSlip(CallbackInfoReturnable<Float> cir) {
        IceSpeed module = IceSpeed.INSTANCE;
        if (!module.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !player.onGround()) return;

        BlockPos below = player.getOnPos();
        BlockState state = player.level().getBlockState(below);
        if (state.getBlock() != (Object) this) return;

        cir.setReturnValue(module.friction());
    }
}
