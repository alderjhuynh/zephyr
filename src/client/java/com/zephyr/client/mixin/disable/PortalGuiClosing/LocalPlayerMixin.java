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

/**
 * Mixin targeting {@link LocalPlayer} that backs the
 * {@code disablePortalGuiClosing} module.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    /**
     * Required constructor for the mixin that satisfies the
     * {@link AbstractClientPlayer} superclass contract.
     *
     * @param world   the client level the player belongs to
     * @param profile the player's game profile
     */
    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Modifies the current screen value used inside
     * {@code LocalPlayer#handlePortalTransitionEffect}: when the module is enabled the
     * expression is replaced with {@code null} so that no screen is closed when
     * travelling through a portal.
     *
     * @param original the currently open screen, or {@code null} if none
     * @return {@code null} to keep the GUI open, otherwise {@code original}
     */
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