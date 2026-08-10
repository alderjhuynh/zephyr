package com.zephyr.client.mixin.qol.FreeCam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link ItemInHandRenderer} that makes the first-person hand render
 * follow the Zephyr FreeCam module's {@link FreeCamera} rather than the local
 * player.
 *
 * <p>Within {@code ItemInHandRenderer.submitHandsWithItems} it redirects the
 * view rotations, bob animation fields, and light coordinates so the hand is
 * drawn, animated, and shaded from the camera's perspective. This keeps the
 * hand in sync with the detached camera when "Show Hand" is enabled.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    /**
     * The partial tick captured at the head of {@code submitHandsWithItems},
     * reused for light coordinate lookups.
     */
    @Unique
    private float zephyr$tickDelta;

    /**
     * Redirects the hand render's X-view rotation to the FreeCamera when
     * FreeCam is active.
     *
     * @param player      the local player being rendered
     * @param partialTick the current partial tick
     * @return the view X rotation to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"
            )
    )
    private float zephyr$redirectGetViewXRot(LocalPlayer player, float partialTick) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().getViewXRot(partialTick) : player.getViewXRot(partialTick);
    }

    /**
     * Redirects the hand render's Y-view rotation to the FreeCamera when
     * FreeCam is active.
     *
     * @param player      the local player being rendered
     * @param partialTick the current partial tick
     * @return the view Y rotation to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"
            )
    )
    private float zephyr$redirectGetViewYRot(LocalPlayer player, float partialTick) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().getViewYRot(partialTick) : player.getViewYRot(partialTick);
    }

    /**
     * Makes the hand's X bob offset depend on FreeCamera movement rather than
     * player movement while FreeCam is active.
     *
     * @param player the local player whose bob would be read
     * @return the X bob value to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;xBob:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetXBob(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().xBob : player.xBob;
    }

    /**
     * Makes the hand's previous X bob offset depend on FreeCamera movement
     * rather than player movement while FreeCam is active.
     *
     * @param player the local player whose bob would be read
     * @return the previous X bob value to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;xBobO:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetXBobO(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().xBobO : player.xBobO;
    }

    /**
     * Makes the hand's Y bob offset depend on FreeCamera movement rather than
     * player movement while FreeCam is active.
     *
     * @param player the local player whose bob would be read
     * @return the Y bob value to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;yBob:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetYBob(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().yBob : player.yBob;
    }

    /**
     * Makes the hand's previous Y bob offset depend on FreeCamera movement
     * rather than player movement while FreeCam is active.
     *
     * @param player the local player whose bob would be read
     * @return the previous Y bob value to use for the hand
     */
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;yBobO:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetYBobO(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().yBobO : player.yBobO;
    }

    /**
     * Stores the partial tick used by {@code submitHandsWithItems} so the light
     * coordinate redirect can query the FreeCamera's packed light.
     *
     * @param partialTick  the current partial tick
     * @param poseStack    the pose stack for the hand render
     * @param nodeCollector the render node collector
     * @param player       the local player being rendered
     * @param packedLight  the packed light coordinates
     * @param ci           mixin callback info (unused)
     */
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void zephyr$storeTickDelta(float partialTick, PoseStack poseStack,
                                       SubmitNodeCollector nodeCollector,
                                       LocalPlayer player,
                                       int packedLight,
                                       CallbackInfo ci) {
        this.zephyr$tickDelta = partialTick;
    }

    /**
     * Makes arm shading depend on the FreeCamera's position rather than the
     * player's position while FreeCam is active.
     *
     * @param lightCoords the original packed light coordinates
     * @return the packed light coordinates to use for the hand
     */
    @ModifyVariable(method = "submitHandsWithItems", at = @At("HEAD"), argsOnly = true)
    private int zephyr$redirectLightCoords(int lightCoords) {
        if (FreeCam.isActive()) {
            return MC.getEntityRenderDispatcher().getPackedLightCoords(FreeCam.getFreeCamera(), zephyr$tickDelta);
        }
        return lightCoords;
    }
}
