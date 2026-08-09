package com.zephyr.client.mixin.qol.Sneak;

import com.zephyr.client.module.qol.Sneak;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$forceSneak(CallbackInfo ci) {
        if (!Sneak.INSTANCE.isEnabled()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        Input current = accessor.zephyr$getKeyPresses();

        accessor.zephyr$setKeyPresses(new Input(
                current.forward(), current.backward(),
                current.left(), current.right(),
                current.jump(), true, current.sprint()
        ));
    }
}