package com.zephyr.client.mixin.combat.AnimeProtagonist;

import com.zephyr.client.module.combat.AnimeProtagonist.AnimeProtagonist;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link MultiPlayerGameMode}. Injects into {@code MultiPlayerGameMode#attack}
 * right before the attack packet is sent via {@code ClientPacketListener#send} to invoke
 * {@link AnimeProtagonist#onAttack()}, spoofing the teleport-behind movement packets just
 * before the swing is transmitted to the server.
 */
@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    /** Triggers the AnimeProtagonist movement packet spoof immediately before the attack packet is sent. */
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void zephyr$sendAnimeProtagonistPacketsBeforeAttack(
            Player player, net.minecraft.world.entity.Entity entity, CallbackInfo ci
    ) {
        AnimeProtagonist.onAttack();
    }
}
