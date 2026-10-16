package com.zephyr.client.mixin.qol.KeybindFix;

import com.mojang.blaze3d.platform.InputConstants;
import com.zephyr.client.module.qol.KeybindFix;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link KeyMapping} implementing the Zephyr KeybindFix module.
 *
 * <p>Vanilla binds every unbound key to {@code key.keyboard.unknown} (value 0):
 * the controls-screen ESC handler, the {@code options.txt} fallback for invalid
 * entries, and even plain loading of a stored {@code key.keyboard.unknown} all
 * funnel through {@link KeyMapping#setKey}. Value 0 is a real SDL scancode, so
 * on Linux a key event with scancode 0 (exotic/multimedia keys) lands in the
 * unknown bucket of the dispatch map and spuriously triggers every "unbound"
 * mapping via {@code click}/{@code set}, which never consult
 * {@link KeyMapping#isUnbound}.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    /** The bound key of this mapping. */
    @Shadow
    protected InputConstants.Key key;

    /**
     * Redirects unbinds to the none sentinel while the module is enabled, so
     * unbound mappings sit in the {@code -1} bucket no physical key can ever
     * produce instead of the {@code 0} bucket real Linux key events can hit.
     *
     * @param key the key vanilla is about to bind
     * @return the none sentinel when vanilla passes the unknown sentinel and
     *         the module is enabled, otherwise the key unchanged
     */
    @ModifyVariable(method = "setKey", at = @At("HEAD"), argsOnly = true)
    private InputConstants.Key zephyr$fixUnboundKey(InputConstants.Key key) {
        if (key != null && KeybindFix.INSTANCE.isEnabled() && InputConstants.UNKNOWN.equals(key)) {
            return KeybindFix.none();
        }
        return key;
    }

    /**
     * Treats the none sentinel as unbound. Unlike the redirect above this is
     * unconditional: a {@code -1} binding is never a real key, however it got
     * there, so it must always report as unbound.
     *
     * @param cir mixin callback used to force the result to {@code true}
     */
    @Inject(method = "isUnbound", at = @At("HEAD"), cancellable = true)
    private void zephyr$treatNoneAsUnbound(CallbackInfoReturnable<Boolean> cir) {
        if (KeybindFix.isNone(this.key)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Keeps the none sentinel out of the ingame-focus key poll. Vanilla skips
     * the unknown sentinel (value 0) here; the none sentinel (value -1) must
     * be skipped too, otherwise the state poll would read scancode -1 out of
     * bounds. Unconditional for the same reason as
     * {@link #zephyr$treatNoneAsUnbound}.
     *
     * @param cir mixin callback used to force the result to {@code false}
     */
    @Inject(method = "shouldSetOnIngameFocus", at = @At("HEAD"), cancellable = true)
    private void zephyr$skipNoneOnIngameFocus(CallbackInfoReturnable<Boolean> cir) {
        if (KeybindFix.isNone(this.key)) {
            cir.setReturnValue(false);
        }
    }
}
