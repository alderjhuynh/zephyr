package com.zephyr.client.mixin.combat.Knockback;

import com.zephyr.client.module.combat.Knockback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link ClientPacketListener}. Injects into the HEAD of
 * {@code ClientPacketListener#handleSetEntityMotion} and cancels it for the local player to
 * back the {@code Knockback} module: incoming knockback velocity is scaled by the configured
 * amount instead of being applied directly.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    /** Scales the local player's set-motion velocity by the Knockback amount and cancels the vanilla application. */
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void zephyr$reduceKnockback(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (!Knockback.INSTANCE.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Entity entity = client.level.getEntity(packet.id());
        if (entity != client.player) return;

        double factor = Knockback.INSTANCE.amount.get();
        Vec3 movement = packet.movement();
        entity.setDeltaMovement(movement.x * factor, movement.y * factor, movement.z * factor);
        ci.cancel();
    }
}
