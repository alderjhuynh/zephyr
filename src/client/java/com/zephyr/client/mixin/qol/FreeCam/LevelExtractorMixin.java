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

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Invoker
    abstract EntityRenderState callExtractEntity(Entity entity, float partialTickTime);

    // Adds the local player to the visible entities while freecam is enabled so they
    // render (showPlayer) and/or get the outline (outlinePlayer).
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
