package com.zephyr.client.mixin.disable.FluidFog;

import com.zephyr.client.module.disable.disableFluidFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link WaterFogEnvironment} that backs the {@code disableFluidFog}
 * module. It handles the water half of the module's behaviour.
 */
@Mixin(WaterFogEnvironment.class)
public class WaterFogEnvironmentMixin {

    /**
     * Intercepts {@code WaterFogEnvironment#setupFog} at its head: when the module is
     * enabled all water fog distances are pushed to {@link Float#MAX_VALUE} so the fog
     * is effectively invisible, and the original fog setup is cancelled.
     *
     * @param fogData      the fog data being populated for the frame
     * @param camera       the current camera
     * @param level        the client level being rendered
     * @param partialTick  the partial tick time
     * @param deltaTracker the tick delta tracker
     * @param ci           the cancellable injection callback
     */
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void zephyr$removeWaterFog(FogData fogData, Camera camera, ClientLevel level, float partialTick, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!disableFluidFog.INSTANCE.isEnabled()) return;
        fogData.environmentalStart = Float.MAX_VALUE;
        fogData.environmentalEnd = Float.MAX_VALUE;
        fogData.skyEnd = Float.MAX_VALUE;
        fogData.cloudEnd = Float.MAX_VALUE;
        ci.cancel();
    }
}
