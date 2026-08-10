package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.zephyr.client.module.qol.FreeCam.MC;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    // Vanilla skips the local player during entity rendering when the camera is detached,
    // which is always the case while freecam is active. This redirects the
    // `camera.getEntity() == entity` check so the player passes and renders while freecam
    // is enabled (for showPlayer and/or outlinePlayer).
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity zephyr$renderLocalPlayer(Camera camera) {
        if (FreeCam.INSTANCE.isEnabled() && MC.player != null && (FreeCam.shouldShowPlayer() || FreeCam.isOutlineEnabled())) {
            return MC.player;
        }
        return camera.getEntity();
    }
}
