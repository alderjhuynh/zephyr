package com.zephyr.client.mixin.commands;

import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.commands.CommandPrefixHandler;
import com.zephyr.client.configplusgui.hud.PartyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ChatInterceptMixin {
    // Runs before the dispatch inject below: when UwU Chat is on, the outgoing message
    // is rewritten in place so the (possibly cancelled) forward path also sees it.
    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String zephyr$uwuChat(String message) {
        if (!PartyManager.uwu) return message;

        String prefix = CommandPrefixHandler.currentPrefix();
        if (prefix != null && !prefix.isEmpty() && message.startsWith(prefix)) {
            return message;
        }
        return PartyManager.uwuify(message);
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void zephyr$interceptChat(String message, CallbackInfo ci) {
        if (CommandManager.dispatch(Minecraft.getInstance(), message)) {
            ci.cancel();
        }
    }
}
