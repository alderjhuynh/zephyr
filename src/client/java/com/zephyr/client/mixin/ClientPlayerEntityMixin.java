package com.zephyr.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zephyr.client.disable.disablePortalGuiClosing;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyExpressionValue(method = "tickNausea", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.GETFIELD))
    private Screen modifyNauseaCurrentScreen(Screen original) {
        if (disablePortalGuiClosing.enabled) return null;
        return original;
    }
}
