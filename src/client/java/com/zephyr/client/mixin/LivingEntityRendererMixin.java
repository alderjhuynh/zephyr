package com.zephyr.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;

import com.zephyr.client.disable.disableFirstPersonEffectParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRendererMixin {

    @Redirect(
            method = "tickStatusEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"
            )
    )
    private void zephyr$skipOwnParticlesInFirstPerson(
            World world,
            ParticleEffect parameters,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (disableFirstPersonEffectParticles.enabled
                && entity instanceof ClientPlayerEntity player
                && player == client.player
                && client.options.getPerspective().isFirstPerson()) {
            return;
        }

        world.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
    }
}
