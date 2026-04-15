package com.zephyr.client.mixin;

import com.zephyr.client.module.F5Tweaks;
import com.zephyr.client.module.FreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow @Final private MinecraftClient client;

    @Redirect(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void f5mod$freeLook(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        Perspective perspective = client.options.getPerspective();
        if (FreeCam.isActive()) {
            FreeCam.rotateCamera((float) cursorDeltaX, (float) cursorDeltaY);
            return;
        }

        if (!F5Tweaks.shouldFreeLook(perspective)) {
            player.changeLookDirection(cursorDeltaX, cursorDeltaY);
            return;
        }

        F5Tweaks.ensureInitialized(player.getYaw(), player.getPitch());
        F5Tweaks.rotateCamera((float)cursorDeltaX, (float)cursorDeltaY);
    }
}
