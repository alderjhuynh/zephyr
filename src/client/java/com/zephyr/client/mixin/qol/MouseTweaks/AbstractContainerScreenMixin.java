package com.zephyr.client.mixin.qol.MouseTweaks;

import com.zephyr.client.module.qol.mousetweaks.MouseTweaksHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks AbstractContainerScreen mouse events to MouseTweaks handler.
 * Implements RMB/LMB drag tweaks and wheel tweak.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void zephyr$mouseTweaksClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseClicked(screen, mouseX, mouseY, button);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void zephyr$mouseTweaksDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseDragged(screen, mouseX, mouseY, button);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void zephyr$mouseTweaksReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseReleased(screen, mouseX, mouseY, button);
    }

    // NOTE: AbstractContainerScreen does not declare mouseScrolled in 1.21.1
    // (scroll handling lives in GuiEventListener/ContainerEventHandler defaults),
    // so injecting mouseScrolled here fails validation and crashes runClient.
    // Wheel-tweak scroll handling is instead wired via Fabric's
    // ScreenMouseEvents.allowMouseScroll in ZephyrClient. See MouseTweaksHandler.onMouseScrolled.
}
