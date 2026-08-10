package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link EntityRenderDispatcher} that controls which entities are
 * rendered while the Zephyr FreeCam module is active.
 *
 * <p>Injects at the head of {@code EntityRenderDispatcher.shouldRender} to
 * always cull the invisible {@link FreeCamera} entity (which would otherwise
 * cast a shadow with Iris/shaders) and to hide the local player when the
 * "Show Player" setting is disabled.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    /**
     * Returns {@code false} for the FreeCamera entity and for the local player
     * when FreeCam's "Show Player" setting is turned off.
     *
     * @param entity the entity being considered for rendering
     * @param culler the active frustum culler
     * @param camX   the camera x position
     * @param camY   the camera y position
     * @param camZ   the camera z position
     * @param cir    mixin callback used to force the render decision
     */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void zephyr$shouldRender(Entity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof FreeCamera) {
            cir.setReturnValue(false);
        } else if (entity == MC.player && FreeCam.INSTANCE.isEnabled() && FreeCam.shouldHidePlayer()) {
            cir.setReturnValue(false);
        }
    }
}
