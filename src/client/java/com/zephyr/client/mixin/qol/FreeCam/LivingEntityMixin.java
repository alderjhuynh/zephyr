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

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract float getHealth();

    // Allows for the horizontal speed of creative flight to be configured separately from vertical speed.
    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true)
    private void zephyr$creativeFlightSpeed(CallbackInfoReturnable<Float> cir) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.getFlightMode().equals(FlightMode.CREATIVE) && freecam$this() == FreeCam.getFreeCamera()) {
            cir.setReturnValue((float) (FreeCam.getHorizontalSpeed() / 10) * (FreeCam.getFreeCamera().isSprinting() ? 2 : 1));
        }
    }

    // Disables freecam upon receiving damage if disableOnDamage is enabled.
    @Inject(method = "setHealth", at = @At("HEAD"))
    private void zephyr$disableOnDamage(float health, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldDisableOnDamage() && freecam$this() == MC.player) {
            if (!MC.player.isCreative() && getHealth() > health) {
                FreeCam.disableNextTick();
            }
        }
    }

    @Unique
    private LivingEntity freecam$this() {
        return (LivingEntity) (Object) this;
    }
}
