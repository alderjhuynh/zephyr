package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link LevelExtractor} that re-adds the local player to the set of
 * visible entities while the Zephyr FreeCam module is active.
 *
 * <p>Since FreeCam detaches the camera from the player, the player may be
 * culled from rendering. Injecting at the tail of
 * {@code LevelExtractor.extractVisibleEntities} re-extracts the player entity
 * so it can still render ("Show Player") and/or receive the outline
 * ("Outline Player").
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    /**
     * Invoker exposing the private {@code extractEntity} method so the mixin can
     * build an {@link EntityRenderState} for the local player.
     *
     * @param entity           the entity to extract
     * @param partialTickTime  the partial tick for interpolation
     * @return the extracted render state
     */
    @Invoker
    abstract EntityRenderState callExtractEntity(Entity entity, float partialTickTime);

    /**
     * Adds the local player to the visible entity render states while FreeCam
     * is enabled and either "Show Player" or "Outline Player" is active. When
     * outlining, the player's team color is applied and outline rendering is
     * requested on the level render state.
     *
     * @param camera            the active camera
     * @param frustum           the frustum used for culling
     * @param deltaTracker      the frame delta tracker for partial ticks
     * @param levelRenderState  the level render state accumulating entity states
     * @param ci                mixin callback info (unused)
     */
    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void zephyr$extractPlayer(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (MC.level != null && FreeCam.INSTANCE.isEnabled() && (FreeCam.shouldShowPlayer() || FreeCam.isOutlineEnabled())) {
            Entity player = MC.player;
            TickRateManager tickRateManager = MC.level.tickRateManager();
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(player));
            EntityRenderState state = callExtractEntity(player, partialTick);
            if (FreeCam.isOutlineEnabled()) {
                state.outlineColor = ARGB.opaque(MC.player.getTeamColor());
                levelRenderState.shouldShowEntityOutlines = true;
            }
            levelRenderState.entityRenderStates.add(state);
        }
    }
}
