package com.zephyr.client.mixin.disable.ExplosionParticles;

import com.zephyr.client.module.disable.disableExplosionParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Mixin targeting {@link ClientLevel} that backs the
 * {@code disableExplosionParticles} module.
 */
@Mixin(ClientLevel.class)
public class ExplosionParticleMixin {

    /**
     * Cancels {@code ClientLevel#addParticle} at its head so that explosion,
     * explosion emitter, poof, and smoke particles are never spawned while the
     * module is enabled.
     *
     * <p>Per vanilla source, an explosion reaches the client as a
     * {@code ClientboundExplodePacket} handled in
     * {@code ClientPacketListener#handleExplosion}, which spawns
     * {@code packet.explosionParticle()} ({@code EXPLOSION} for small radii,
     * {@code EXPLOSION_EMITTER} otherwise) via {@code addParticle} and queues
     * {@code packet.blockParticles()} (vanilla default: {@code POOF} and
     * {@code SMOKE}, from {@code Level#DEFAULT_EXPLOSION_BLOCK_PARTICLES})
     * through {@code ClientExplosionTracker}, which also emits them via
     * {@code addParticle}.
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
    private void zephyr$disableExplosionParticles(ParticleOptions options, double x, double y, double z,
                                                  double velocityX, double velocityY, double velocityZ,
                                                  CallbackInfo ci) {
        if (!disableExplosionParticles.INSTANCE.isEnabled()) {return;}
        if (isExplosionParticle(options)) {
            ci.cancel();
        }
    }

    /**
     * Cancels the {@code ClientLevel#addParticle} overload with distance and
     * force flags so that server-sent explosion particles are also hidden
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
    private void zephyr$disableExplosionParticlesForced(ParticleOptions options, boolean force, boolean decreased,
                                                        double x, double y, double z,
                                                        double velocityX, double velocityY, double velocityZ,
                                                        CallbackInfo ci) {
        if (!disableExplosionParticles.INSTANCE.isEnabled()) {return;}
        if (isExplosionParticle(options)) {
            ci.cancel();
        }
    }

    private static boolean isExplosionParticle(ParticleOptions options) {
        if (options == null) return false;
        return options.getType() == ParticleTypes.EXPLOSION
                || options.getType() == ParticleTypes.EXPLOSION_EMITTER
                || options.getType() == ParticleTypes.POOF
                || options.getType() == ParticleTypes.SMOKE;
    }
}
