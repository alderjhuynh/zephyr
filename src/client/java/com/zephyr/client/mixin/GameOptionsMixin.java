package com.zephyr.client.mixin;

import com.zephyr.client.module.F5Tweaks;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow private Perspective perspective;

    @ModifyVariable(method = "setPerspective", at = @At("HEAD"), argsOnly = true)
    private Perspective f5mod$forceToggle(Perspective perspective) {
        if (!F5Tweaks.enabled) {
            return perspective;
        }

        return perspective == Perspective.THIRD_PERSON_FRONT
                ? Perspective.FIRST_PERSON
                : perspective;
    }

    @Inject(method = "setPerspective", at = @At("TAIL"))
    private void f5mod$trackPerspective(Perspective perspective, CallbackInfo ci) {
        F5Tweaks.onPerspectiveChanged(this.perspective);
    }
}
