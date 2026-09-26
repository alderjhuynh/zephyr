package com.zephyr.client.mixin.qol.RenderInvisibility;

import com.zephyr.client.module.qol.RenderInvisibility;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin into {@link Entity} implementing the Zephyr RenderInvisibility module.
 *
 * <p>Redirects the {@code Entity.isInvisible()} call inside
 * {@code Entity.isInvisibleTo} so invisible entities are always reported as
 * visible while the module is enabled, leaving their (translucent) rendering
 * intact instead of being hidden entirely.
 */
@Mixin(Entity.class)
public abstract class InvisibilityRenderMixin {

    /**
     * Forces invisible entities to be treated as visible while the
     * RenderInvisibility module is enabled.
     *
     * @param entity the entity whose invisibility is being checked
     * @return {@code false} when the module is enabled, otherwise the original
     *         invisibility state
     */
    @Redirect(
            method = "isInvisibleTo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"
            )
    )
    private boolean zephyr$alwaysVisible(Entity entity) {
        if (RenderInvisibility.INSTANCE.isEnabled()) {
            return false;
        }
        return entity.isInvisible();
    }
}