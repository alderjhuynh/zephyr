package com.zephyr.client.mixin.module.qol.RenderInvisibility;

import com.zephyr.client.module.qol.RenderInvisibility;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class InvisibilityRenderMixin {

    @Redirect(
            method = "isInvisibleTo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isInvisible()Z"
            )
    )
    private boolean zephyr$alwaysVisible(Entity entity) {
        if (!RenderInvisibility.enabled) {
            return entity.isInvisible();
        }
        return false;
    }
}