package com.zephyr.client.mixin.qol.KeybindFix;

import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.module.qol.KeybindFix;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link InputConstants.Key} implementing the display half of the
 * Zephyr KeybindFix module.
 *
 * <p>Without this, the none sentinel ({@code key.keyboard.none}, value -1)
 * would fall into vanilla's SDL key-name lookup with an invalid scancode.
 * Like the {@code KeyMapping} recognizers this is unconditional: the sentinel
 * must always render as "Not Bound", however it got bound.
 */
@Mixin(InputConstants.Key.class)
public abstract class KeyDisplayMixin {
    /**
     * Renders the none sentinel via its own translation key instead of the
     * SDL key-name lookup.
     *
     * @param cir mixin callback used to return the translated "Not Bound"
     *            text for the sentinel
     */
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void zephyr$displayNoneAsNotBound(CallbackInfoReturnable<Component> cir) {
        InputConstants.Key self = (InputConstants.Key) (Object) this;
        if (KeybindFix.isNone(self)) {
            cir.setReturnValue(Component.translatable(KeybindFix.NONE_NAME));
        }
    }
}
