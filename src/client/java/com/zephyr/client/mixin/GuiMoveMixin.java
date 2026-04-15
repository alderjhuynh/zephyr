package com.zephyr.client.mixin;

import com.zephyr.client.module.GuiMove;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class GuiMoveMixin {
    @Shadow public GameOptions options;
    @Shadow public Screen currentScreen;
    @Shadow public abstract Window getWindow();

    @Unique private boolean zephyr$managedGuiMoveInput;
    @Unique private boolean zephyr$forwardPressed;
    @Unique private boolean zephyr$backPressed;
    @Unique private boolean zephyr$leftPressed;
    @Unique private boolean zephyr$rightPressed;
    @Unique private boolean zephyr$jumpPressed;
    @Unique private boolean zephyr$sprintPressed;

    @Inject(method = "tick", at = @At("HEAD"))
    private void zephyr$syncGuiMoveKeys(CallbackInfo ci) {
        if (this.options == null) {
            return;
        }

        if (this.currentScreen != null) {
            if (!GuiMove.enabled) {
                this.zephyr$applyTrackedStates(false, false, false, false, false, false);
                this.zephyr$managedGuiMoveInput = false;
                return;
            }

            Window window = this.getWindow();
            this.zephyr$applyTrackedStates(
                    this.zephyr$isPhysicalKeyPressed(this.options.forwardKey, window),
                    this.zephyr$isPhysicalKeyPressed(this.options.backKey, window),
                    this.zephyr$isPhysicalKeyPressed(this.options.leftKey, window),
                    this.zephyr$isPhysicalKeyPressed(this.options.rightKey, window),
                    this.zephyr$isPhysicalKeyPressed(this.options.jumpKey, window),
                    this.zephyr$isPhysicalKeyPressed(this.options.sprintKey, window)
            );
            this.zephyr$managedGuiMoveInput = true;
            return;
        }

        if (!this.zephyr$managedGuiMoveInput) {
            return;
        }

        Window window = this.getWindow();
        this.zephyr$applyTrackedStates(
                this.zephyr$isPhysicalKeyPressed(this.options.forwardKey, window),
                this.zephyr$isPhysicalKeyPressed(this.options.backKey, window),
                this.zephyr$isPhysicalKeyPressed(this.options.leftKey, window),
                this.zephyr$isPhysicalKeyPressed(this.options.rightKey, window),
                this.zephyr$isPhysicalKeyPressed(this.options.jumpKey, window),
                this.zephyr$isPhysicalKeyPressed(this.options.sprintKey, window)
        );
        this.zephyr$managedGuiMoveInput = false;
    }

    @Unique
    private boolean zephyr$isPhysicalKeyPressed(KeyBinding keyBinding, Window window) {
        InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).zephyr$getBoundKey();
        if (boundKey == null || keyBinding.isUnbound() || boundKey.getCategory() != InputUtil.Type.KEYSYM) {
            return false;
        }

        return InputUtil.isKeyPressed(window.getHandle(), boundKey.getCode());
    }

    @Unique
    private void zephyr$applyTrackedStates(
            boolean forwardPressed,
            boolean backPressed,
            boolean leftPressed,
            boolean rightPressed,
            boolean jumpPressed,
            boolean sprintPressed
    ) {
        this.zephyr$setPressed(this.options.forwardKey, this.zephyr$forwardPressed, forwardPressed);
        this.zephyr$setPressed(this.options.backKey, this.zephyr$backPressed, backPressed);
        this.zephyr$setPressed(this.options.leftKey, this.zephyr$leftPressed, leftPressed);
        this.zephyr$setPressed(this.options.rightKey, this.zephyr$rightPressed, rightPressed);
        this.zephyr$setPressed(this.options.jumpKey, this.zephyr$jumpPressed, jumpPressed);
        this.zephyr$setPressed(this.options.sprintKey, this.zephyr$sprintPressed, sprintPressed);

        this.zephyr$forwardPressed = forwardPressed;
        this.zephyr$backPressed = backPressed;
        this.zephyr$leftPressed = leftPressed;
        this.zephyr$rightPressed = rightPressed;
        this.zephyr$jumpPressed = jumpPressed;
        this.zephyr$sprintPressed = sprintPressed;
    }

    @Unique
    private void zephyr$setPressed(KeyBinding keyBinding, boolean previousState, boolean currentState) {
        if (previousState != currentState) {
            keyBinding.setPressed(currentState);
        }
    }
}
