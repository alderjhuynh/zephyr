package com.zephyr.client.mixin.qol.AutoTool;

import com.zephyr.client.module.qol.AutoTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class AttackBlockMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) return;
        if (!AutoTool.INSTANCE.isEnabled())  return;

        ClientLevel world = client.level;
        BlockState state = world.getBlockState(pos);

        AutoTool.onStartBreakingBlock(state);
    }
}