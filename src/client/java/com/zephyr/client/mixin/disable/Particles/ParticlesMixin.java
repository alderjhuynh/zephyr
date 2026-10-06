package com.zephyr.client.mixin.disable.Particles;

import com.zephyr.client.module.disable.disableParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Mixin targeting {@link ClientLevel} that backs the
 * {@code disableParticles} module.
 */
@Mixin(ClientLevel.class)
public class ParticlesMixin {

    /**
     * Cancels {@code ClientLevel#addParticle} at its head so that any particle
     * type named in the module's list setting is never spawned while the
     * module is enabled.
     *
     * @param options the particle options describing the particle
     * @param x       the particle's x position
     * @param y       the particle's y position
     * @param z       the particle's z position
     * @param velocityX the particle's x velocity
     * @param velocityY the particle's y velocity
     * @param velocityZ the particle's z velocity
     * @param ci      the cancellable injection callback
     */
    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$disableListedParticles(ParticleOptions options, double x, double y, double z,
                                               double velocityX, double velocityY, double velocityZ,
                                               CallbackInfo ci) {
        if (!disableParticles.INSTANCE.isEnabled()) {return;}
        if (disableParticles.INSTANCE.shouldDisable(options)) {
            ci.cancel();
        }
    }

    /**
     * Cancels the {@code ClientLevel#addParticle} overload with distance and
     * force flags so that server-sent listed particles are also hidden
     * while the module is enabled.
     *
     * @param options the particle options describing the particle
     * @param force   whether the particle bypasses distance culling
     * @param decreased whether the particle uses decreased rendering past the cull distance
     * @param x       the particle's x position
     * @param y       the particle's y position
     * @param z       the particle's z position
     * @param velocityX the particle's x velocity
     * @param velocityY the particle's y velocity
     * @param velocityZ the particle's z velocity
     * @param ci      the cancellable injection callback
     */
    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$disableListedParticlesForced(ParticleOptions options, boolean force, boolean decreased,
                                                     double x, double y, double z,
                                                     double velocityX, double velocityY, double velocityZ,
                                                     CallbackInfo ci) {
        if (!disableParticles.INSTANCE.isEnabled()) {return;}
        if (disableParticles.INSTANCE.shouldDisable(options)) {
            ci.cancel();
        }
    }
}
