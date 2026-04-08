package com.zephyr.client.mixin;

import com.zephyr.client.gui.ZephyrMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCustomButton(CallbackInfo ci) {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int margin = 8;
        int x = this.width - buttonWidth - margin;
        int y = margin;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Zephyr Menu"),
                button -> this.client.setScreen(new ZephyrMenuScreen(this))
        ).dimensions(x, y, buttonWidth, buttonHeight).build());
    }
}
