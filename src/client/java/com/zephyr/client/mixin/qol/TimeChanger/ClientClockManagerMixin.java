package com.zephyr.client.mixin.qol.TimeChanger;

import com.zephyr.client.module.qol.TimeChanger;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin into {@link Level} implementing the Zephyr TimeChanger module.
 *
 * <p>In 26.3 per-clock times moved onto clock instances queried through
 * {@code Level.getClockTimeTicks}. Injecting at its head lets the overworld
 * clock return the configured time of day instead of the server-synced value
 * while the module is enabled, giving a purely client-side time change.
 */
@Mixin(Level.class)
public class ClientClockManagerMixin {

    /**
     * Overrides the overworld clock time with the TimeChanger module's
     * configured value while enabled.
     *
     * @param clock the clock being queried
     * @param cir   mixin callback used to substitute the total tick count
     */
    @Inject(method = "getClockTimeTicks", at = @At("HEAD"), cancellable = true)
    private void zephyr$changeTime(Optional<? extends Holder<WorldClock>> clock, CallbackInfoReturnable<Long> cir) {
        if (!TimeChanger.INSTANCE.isEnabled()) return;
        if (clock.map(holder -> holder.is(WorldClocks.OVERWORLD)).orElse(false)) {
            cir.setReturnValue((long) (double) TimeChanger.INSTANCE.time.get());
        }
    }
}
