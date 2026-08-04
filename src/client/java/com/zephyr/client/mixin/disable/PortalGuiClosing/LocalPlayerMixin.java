package com.zephyr.client.mixin.disable.PortalGuiClosing;

import com.zephyr.client.module.disable.disablePortalGuiClosing;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @ModifyExpressionValue(
            method = "handlePortalTransitionEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;screen()Lnet/minecraft/client/gui/screens/Screen;"
            )
    )
    private Screen zephyr$modifyPortalTransitionEffect(Screen original) {
        if (disablePortalGuiClosing.INSTANCE.isEnabled()) {
            return null;
        }
        return original;
    }
}