package com.zephyr.client.mixin.combat.AnimeProtagonist;

import com.zephyr.client.module.combat.AnimeProtagonist.TargetManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link MultiPlayerGameMode}. Injects into the HEAD of {@code MultiPlayerGameMode#attack}
 * to record the entity being attacked into {@link TargetManager}, so the AnimeProtagonist
 * module knows where to teleport behind.
 */
@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {

    /** Records the attacked living entity (or clears the target for non-living entities) before the attack executes. */
    @Inject(
        method = "attack",
        at = @At("HEAD")
    )
    private void zephyr$storeTarget(Player player, Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            TargetManager.setTarget(living);
        } else {
            TargetManager.clear();
        }
    }
}