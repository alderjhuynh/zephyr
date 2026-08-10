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
import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * While a screen is open, the game no longer routes raw key events into the
     * bound {@link KeyMapping}s, so the vanilla computation at the top of this
     * method would normally produce an all-false input state. We replace that
     * computation with one driven directly by the physical keyboard state so
     * movement keeps responding, and cancel the vanilla body entirely.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zephyr$moveWhileGuiOpen(CallbackInfo ci) {
        if (!GuiMove.INSTANCE.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Screen screen = client.screen;
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

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        boolean shift = ((Input) (Object) this).shiftKeyDown;

        accessor.zephyr$setUp(forward);
        accessor.zephyr$setDown(backward);
        accessor.zephyr$setLeft(left);
        accessor.zephyr$setRight(right);
        accessor.zephyr$setJumping(jump);
        accessor.zephyr$setShiftKeyDown(shift);
        accessor.zephyr$setForwardImpulse(zephyr$calculateImpulse(forward, backward));
        accessor.zephyr$setLeftImpulse(zephyr$calculateImpulse(left, right));

        ci.cancel();
    }

    private static float zephyr$calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }

        return positive ? 1.0f : -1.0f;
    }

    private static boolean zephyr$isPhysicallyDown(KeyMapping keyMapping, Window window) {
        if (keyMapping.isUnbound()) {
            return false;
        }

        InputConstants.Key boundKey = ((KeyMappingAccessor) keyMapping).zephyr$getKey();
        if (boundKey.getType() != InputConstants.Type.KEYSYM) {
            return false;
        }

        return InputConstants.isKeyDown(window.getWindow(), boundKey.getValue());
    }
}