package com.zephyr.client.mixin.disable.FirstPersonEffectParticles;

import com.zephyr.client.module.disable.disableFirstPersonEffectParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRendererMixin {

    @Redirect(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void zephyr$skipOwnParticlesInFirstPerson(
            Level world,
            ParticleOptions parameters,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Minecraft client = Minecraft.getInstance();

        if (disableFirstPersonEffectParticles.INSTANCE.isEnabled()
                && entity instanceof LocalPlayer player
                && player == client.player
                && client.options.getCameraType().isFirstPerson()) {
            return;
        }

        world.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
    }
}
