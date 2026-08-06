package com.zephyr.client.mixin.commands;

import com.zephyr.client.commands.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ChatInterceptMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void zephyr$interceptChat(String message, CallbackInfo ci) {
        if (CommandManager.dispatch(Minecraft.getInstance(), message)) {
            ci.cancel();
        }
    }
}
