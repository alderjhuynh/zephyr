package com.zephyr.client.mixin.disable.PortalGuiClosing;

import com.zephyr.client.module.disable.disablePortalGuiClosing;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @ModifyExpressionValue(
            method = "handleConfusionTransitionEffect",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"
            )
    )
    private Screen zephyr$modifyPortalTransitionEffect(Screen original) {
        if (disablePortalGuiClosing.INSTANCE.isEnabled()) {
            return null;
        }
        return original;
    }
}
