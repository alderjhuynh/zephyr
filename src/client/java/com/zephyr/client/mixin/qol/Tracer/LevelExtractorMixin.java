package com.zephyr.client.mixin.qol.Tracer;

import com.zephyr.client.module.qol.Tracer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks Tracer rendering into the per-frame gizmo collection window.
 *
 * <p>{@code Minecraft.renderFrame()} opens {@code LevelExtractor.collectPerFrameMainThreadGizmos()}
 * around {@code GameRenderer.extract() -> LevelExtractor.extract()}. Injecting just before
 * {@code LevelExtractor.extractGizmos()} ensures the tracer lines are added to
 * {@code LevelExtractor.mainThreadGizmos} and drained into {@code LevelRenderer}
 * in the same frame, yielding render-framerate (60-144Hz) smoothness instead of
 * the 20Hz tick stutter from {@code Minecraft.collectPerTickGizmos()}.</p>
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractGizmos()V"))
    private void zephyr$tracerPerFrame(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        // partialTick here == deltaTracker.getGameTimeDeltaPartialTick(false) as passed from GameRenderer.extract
        // Use the passed float directly; it matches the interpolation factor used for entity extraction.
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            Tracer.INSTANCE.renderPerFrame(client, partialTick);
        }
    }
}
