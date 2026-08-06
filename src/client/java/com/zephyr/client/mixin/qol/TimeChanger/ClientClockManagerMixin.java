package com.zephyr.client.mixin.qol.TimeChanger;

import com.zephyr.client.module.qol.TimeChanger;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void zephyr$changeTime(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        if (!TimeChanger.INSTANCE.isEnabled()) return;
        if (clock.is(WorldClocks.OVERWORLD)) {
            cir.setReturnValue((long) (double) TimeChanger.INSTANCE.time.get());
        }
    }
}
