package com.zephyr.client.mixin.disable.ExplosionParticles;

import com.zephyr.client.module.disable.disableExplosionParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's ExplosionParticleMixin. 1.21.1 ClientLevel has two
// addParticle overloads: (ParticleOptions, DDDDDD) and
// (ParticleOptions, ZDDDDDD). 26.3's second overload takes (ZZDDDDDD).
@Mixin(ClientLevel.class)
public class ExplosionParticleMixin {

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

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zephyr$disableExplosionParticlesForced(ParticleOptions options, boolean force,
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
