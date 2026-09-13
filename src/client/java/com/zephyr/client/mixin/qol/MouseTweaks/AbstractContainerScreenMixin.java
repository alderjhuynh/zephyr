package com.zephyr.client.mixin.qol.MouseTweaks;

import com.zephyr.client.module.qol.mousetweaks.MouseTweaksHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
    private void zephyr$mouseTweaksClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseClicked(screen, event.x(), event.y(), event.button());
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void zephyr$mouseTweaksDragged(MouseButtonEvent event, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseDragged(screen, event.x(), event.y(), event.button());
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void zephyr$mouseTweaksReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        MouseTweaksHandler.onMouseReleased(screen, event.x(), event.y(), event.button());
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void zephyr$mouseTweaksScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        double delta = scrollY != 0 ? scrollY : scrollX;
        boolean handled = MouseTweaksHandler.onMouseScrolled(screen, mouseX, mouseY, delta);
        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
