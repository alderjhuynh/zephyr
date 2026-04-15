package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableNauseaOverlay;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class NauseaOverlayMixin {

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastNauseaIntensity:F"
            )
    )
    private float zephyr$hideLastNauseaIntensity(ClientPlayerEntity player) {
        return disableNauseaOverlay.enabled ? 0.0F : player.lastNauseaIntensity;
    }

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;nauseaIntensity:F"
            )
    )
    private float zephyr$hideNauseaIntensity(ClientPlayerEntity player) {
        return disableNauseaOverlay.enabled ? 0.0F : player.nauseaIntensity;
    }

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
            )
    )
    private float zephyr$hideNauseaEffectFadeFactor(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect, float tickProgress) {
        return disableNauseaOverlay.enabled ? 0.0F : player.getEffectFadeFactor(effect, tickProgress);
    }
}
