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

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void zephyr$reduceKnockback(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (!Knockback.INSTANCE.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Entity entity = client.level.getEntity(packet.getId());
        if (entity != client.player) return;

        double factor = Knockback.INSTANCE.amount.get();
        Vec3 movement = packet.getMovement();
        entity.setDeltaMovement(movement.x * factor, movement.y * factor, movement.z * factor);
        ci.cancel();
    }
}
