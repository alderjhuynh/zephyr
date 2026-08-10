package com.zephyr.client.mixin.qol.GuiMove;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.zephyr.client.mixin.qol.GuiMove.KeyMappingAccessor;
import com.zephyr.client.module.qol.GuiMove;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link KeyboardInput} implementing the Zephyr GuiMove module.
 *
 * <p>Injects at the head of {@code KeyboardInput.tick}. While the module is
 * enabled and a screen is open, the vanilla input computation is replaced with
 * one driven directly by the physical keyboard state (bypassing the
 * {@link KeyMapping} event pipeline, which is inactive for GUIs), and the
 * vanilla body is cancelled so the stale {@code moveVector} calculation never
 * runs.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * While a screen is open, the game no longer routes raw key events into the
     * bound {@link KeyMapping}s, so the vanilla computation at the top of this
     * method would normally produce an all-false {@link Input}. We replace that
     * computation with one driven directly by the physical keyboard state so
     * movement keeps responding, and cancel the vanilla body so its now-stale
     * {@code moveVector} calculation never runs.
     *
     * @param ci mixin callback used to cancel the vanilla input computation
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zephyr$moveWhileGuiOpen(CallbackInfo ci) {
        if (!GuiMove.INSTANCE.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Screen screen = client.gui.screen();
        if (screen == null) {
            return;
        }

        Window window = client.getWindow();
        Options options = client.options;
        boolean ignoreJump = screen instanceof ChatScreen;

        boolean forward = zephyr$isPhysicallyDown(options.keyUp, window);
        boolean backward = zephyr$isPhysicallyDown(options.keyDown, window);
        boolean left = zephyr$isPhysicallyDown(options.keyLeft, window);
        boolean right = zephyr$isPhysicallyDown(options.keyRight, window);
        boolean jump = !ignoreJump && zephyr$isPhysicallyDown(options.keyJump, window);
        boolean sprint = zephyr$isPhysicallyDown(options.keySprint, window);

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        boolean shift = accessor.zephyr$getKeyPresses().shift();

        accessor.zephyr$setKeyPresses(new Input(forward, backward, left, right, jump, shift, sprint));

        float forwardImpulse = zephyr$calculateImpulse(forward, backward);
        float leftImpulse = zephyr$calculateImpulse(left, right);
        if (GuiMove.INSTANCE.isEnabled()) {
            accessor.zephyr$setMoveVector(new Vec2(leftImpulse, forwardImpulse).normalized());
        }

        ci.cancel();
    }

    /**
     * Converts a positive/negative key pair into a movement impulse of -1, 0,
     * or +1.
     *
     * @param positive whether the positive direction key is held
     * @param negative whether the negative direction key is held
     * @return 0 when both or neither are held, otherwise 1 for positive and -1
     *         for negative
     */
    private static float zephyr$calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }

        return positive ? 1.0f : -1.0f;
    }

    /**
     * Whether the physical key bound to the given mapping is currently held
     * down. Only keyboard (KEYSYM) bindings are considered.
     *
     * @param keyMapping the movement key mapping to check
     * @param window     the current window for key state lookup
     * @return true if the bound physical key is down
     */
    private static boolean zephyr$isPhysicallyDown(KeyMapping keyMapping, Window window) {
        if (keyMapping.isUnbound()) {
            return false;
        }

        InputConstants.Key boundKey = ((KeyMappingAccessor) keyMapping).zephyr$getKey();
        if (boundKey.getType() != InputConstants.Type.KEYSYM) {
            return false;
        }

        return InputConstants.isKeyDown(window, boundKey.getValue());
    }
}