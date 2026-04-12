package com.zephyr.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.zephyr.client.module.RenderInvisibility;

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