package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link FirstPersonHandsAndItems} that makes the first-person hand render
 * follow the Zephyr FreeCam module's {@link FreeCamera} rather than the local
 * player.
 *
 * <p>In 26.3 hand rendering moved to render-state extraction: view rotations and
 * bob offsets are captured in
 * {@code FirstPersonHandsAndItems#extractRenderState} instead of being read
 * directly from the player during submission. Injecting at the tail lets the
 * camera's perspective replace the player's when FreeCam is active, keeping the
 * hand in sync with the detached camera when "Show Hand" is enabled.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class ItemInHandRendererMixin {
    /**
     * Replaces the extracted hand view rotations and bob offsets with the
     * FreeCamera's while FreeCam is active.
     *
     * @param player       the local player the state was extracted from
     * @param partialTicks the current partial tick
     * @param state        the hand render state to adjust
     * @param ci           mixin callback info (unused)
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zephyr$useFreeCameraView(LocalPlayer player, float partialTicks,
                                          FirstPersonHandsAndItemsRenderState state,
                                          CallbackInfo ci) {
        if (!FreeCam.isActive()) {
            return;
        }
        FreeCamera camera = FreeCam.getFreeCamera();
        state.viewXRot = camera.getViewXRot(partialTicks);
        state.viewYRot = camera.getViewYRot(partialTicks);
        state.xBob = camera.xBob;
        state.yBob = camera.yBob;
    }
}
