package com.zephyr.client.mixin.combat.Criticals;

import com.zephyr.client.module.combat.Criticals;
import com.zephyr.client.module.movement.NoFall;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link MultiPlayerGameMode}. Injects into {@code MultiPlayerGameMode#attack}
 * right before the attack packet is sent via {@code ClientPacketListener#send} to invoke
 * {@link Criticals#onAttack()}, spoofing the fall packets that produce critical hits. If the
 * {@code NoFall} module is enabled it is temporarily disabled first so its packets do not
 * cancel the spoofed fall.
 */
@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    /** Sends the critical fall packets (pausing {@code NoFall} if active) immediately before the attack packet. */
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void zephyr$sendCriticalPacketsBeforeAttack(
            Player player, net.minecraft.world.entity.Entity entity, CallbackInfo ci
    ) {
        if (NoFall.INSTANCE.isEnabled()) {
            NoFall.INSTANCE.setEnabled(false);
            Criticals.onAttack();
            NoFall.INSTANCE.setEnabled(true);
        } else {
            Criticals.onAttack();
        }

    }
}
