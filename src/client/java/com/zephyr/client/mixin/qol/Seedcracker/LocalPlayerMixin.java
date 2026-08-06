package com.zephyr.client.mixin.qol.Seedcracker;

import com.zephyr.client.module.qol.Seedcracker;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        Seedcracker.get().getDataStorage().tick();
    }
}
