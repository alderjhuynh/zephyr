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

/**
 * Mixin targeting {@link LivingEntity} that backs the
 * {@code disableFirstPersonEffectParticles} module.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityRendererMixin {

    /**
     * Redirects the {@code Level.addParticle} call inside {@code LivingEntity#tickEffects}
     * so that ambient status-effect particles are not spawned for the local player while
     * playing in first person and the module is enabled. All other cases are forwarded
     * to the original method.
     *
     * @param world      the level the particle would be spawned in
     * @param parameters the particle options describing the effect particle
     * @param x          the particle's x position
     * @param y          the particle's y position
     * @param z          the particle's z position
     * @param velocityX  the particle's x velocity
     * @param velocityY  the particle's y velocity
     * @param velocityZ  the particle's z velocity
     */
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
