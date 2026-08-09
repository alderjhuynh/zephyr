package com.zephyr.client.mixin.qol.RenderInvisibility;

import com.zephyr.client.module.qol.RenderInvisibility;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class InvisibilityRenderMixin {

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