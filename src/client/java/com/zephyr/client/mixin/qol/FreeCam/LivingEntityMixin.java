package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FlightMode;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link LivingEntity} with two Zephyr FreeCam behaviors: honoring a
 * separate creative-flight horizontal speed for the FreeCamera and auto
 * disabling the module when the player takes damage.
 *
 * <p>Backs the FreeCam "Flight Mode: CREATIVE" and "Disable on Damage"
 * settings.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Shadows {@link LivingEntity#getHealth()} so the mixin can compare health
     * before and after a {@code setHealth} call.
     *
     * @return the current health of the entity
     */
    @Shadow
    public abstract float getHealth();

    /**
     * Allows the horizontal speed of creative flight to be configured
     * separately from the vertical speed: when FreeCam's flight mode is
     * {@link FlightMode#CREATIVE}, the FreeCamera's friction-influenced speed
     * is replaced by the configured horizontal speed (doubled while sprinting).
     *
     * @param cir mixin callback used to override the speed
     */
    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true)
    private void zephyr$creativeFlightSpeed(CallbackInfoReturnable<Float> cir) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.getFlightMode().equals(FlightMode.CREATIVE) && freecam$this() == FreeCam.getFreeCamera()) {
            cir.setReturnValue((float) (FreeCam.getHorizontalSpeed() / 10) * (FreeCam.getFreeCamera().isSprinting() ? 2 : 1));
        }
    }

    /**
     * Disables FreeCam at the next tick when the local player loses health (and
     * is not in creative) if "Disable on Damage" is enabled.
     *
     * @param health the new health value being set
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "setHealth", at = @At("HEAD"))
    private void zephyr$disableOnDamage(float health, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldDisableOnDamage() && freecam$this() == MC.player) {
            if (!MC.player.isCreative() && getHealth() > health) {
                FreeCam.disableNextTick();
            }
        }
    }

    /**
     * Casts the mixin {@code this} reference back to {@link LivingEntity}.
     *
     * @return this object as a living entity
     */
    @Unique
    private LivingEntity freecam$this() {
        return (LivingEntity) (Object) this;
    }
}
