package com.zephyr.client.mixin.movement.NoSlowdown;

import com.zephyr.client.module.movement.NoSlowdown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link net.minecraft.world.entity.player.Player}. Injects at
 * the head of {@code makeStuckInBlock()} and cancels the call for the local
 * player when the No Slowdown module is enabled, preventing cobwebs from slowing
 * the player down.
 */
@Mixin(Player.class)
public abstract class WebMixin {
    /** Skips the stuck-in-block slowdown (e.g. cobwebs) when No Slowdown is enabled. */
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void zephyr$cancelWebSlowdown(BlockState state, Vec3 speedMultiplier, CallbackInfo ci) {
        if (NoSlowdown.INSTANCE.isEnabled() && (Object) this == Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
