package com.zephyr.client.mixin.movement.Blink;

import com.mojang.authlib.GameProfile;
import com.zephyr.client.module.movement.Blink;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link LocalPlayer} backing the {@code Blink} module. Injects at the head of
 * {@code LocalPlayer.sendPosition} and cancels it while the module is enabled, so the
 * player's position and rotation packets are never sent and the server keeps the player at
 * their pre-blink position.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    /**
     * Constructor required by the {@link AbstractClientPlayer} superclass.
     *
     * @param world   the client level
     * @param profile the player's game profile
     */
    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Suppresses outgoing position packets while Blink is enabled.
     *
     * @param ci mixin callback used to cancel the packet send
     */
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void zephyr$blink(CallbackInfo ci) {
        if (Blink.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
