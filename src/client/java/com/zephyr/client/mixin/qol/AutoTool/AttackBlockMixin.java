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

/**
 * Mixin into {@link MultiPlayerGameMode} that observes when the player starts
 * breaking a block and forwards the target block to the Zephyr AutoTool module.
 *
 * <p>Injects at the head of {@code MultiPlayerGameMode.startDestroyBlock} so the
 * hottest tool can be swapped in before the mining action is processed by the
 * server.
 */
@Mixin(MultiPlayerGameMode.class)
public class AttackBlockMixin {

    /**
     * Invoked when the player begins destroying a block; hands the block's
     * {@link BlockState} to {@code AutoTool.onStartBreakingBlock} so it can
     * select the fastest hotbar tool.
     *
     * @param pos       the position of the block being destroyed
     * @param direction the face of the block being attacked
     * @param cir       mixin callback info (unused)
     */
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