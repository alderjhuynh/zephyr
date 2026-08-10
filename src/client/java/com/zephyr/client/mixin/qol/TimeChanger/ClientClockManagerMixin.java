package com.zephyr.client.mixin.qol.TimeChanger;

import com.zephyr.client.module.qol.TimeChanger;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class ClientClockManagerMixin {

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void zephyr$changeTime(CallbackInfoReturnable<Long> cir) {
        if (!TimeChanger.INSTANCE.isEnabled()) return;
        cir.setReturnValue((long) (double) TimeChanger.INSTANCE.time.get());
    }
}
