package com.zephyr.client.mixin.qol.KeybindFix;

import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.module.qol.KeybindFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link InputConstants.Type} implementing the Zephyr KeybindFix
 * module.
 *
 * <p>Registers the genuine "no key" sentinel ({@code key.keyboard.none},
 * value -1) on the keyboard type. Vanilla only knows
 * {@code key.keyboard.unknown} (value 0), which collides with real SDL
 * scancode 0 key events on Linux. Registration runs unconditionally at class
 * load so {@code options.txt} entries referencing the sentinel always resolve;
 * the module toggle only controls whether new unbinds use it (see
 * {@link KeyMappingMixin}).
 */
@Mixin(InputConstants.Type.class)
public abstract class InputTypeMixin {
    /** Mirrors the private static registrar used by the type's static initializer. */
    @Shadow
    private static void addKey(InputConstants.Type type, String name, int value) {
    }

    /**
     * Adds the none sentinel after vanilla registers its own keys.
     *
     * @param ci mixin callback (static-initializer injection carries no return)
     */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void zephyr$registerNoneKey(CallbackInfo ci) {
        addKey(InputConstants.Type.KEYBOARD, KeybindFix.NONE_NAME, KeybindFix.NONE_VALUE);
    }
}
