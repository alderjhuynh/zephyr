package com.zephyr.client.mixin.combat.AnimeProtagonist;

import com.zephyr.client.module.combat.AnimeProtagonist.AnimeProtagonist;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
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
