package com.zephyr.client.mixin.qol.AppleSkin;

import com.zephyr.client.module.qol.AppleSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "extractFood", at = @At("HEAD"))
    private void zephyr$appleSkinExtractFoodPre(GuiGraphicsExtractor extractor, Player player, int top, int right, CallbackInfo ci) {
        AppleSkin.INSTANCE.onExtractFoodPre(extractor, player, top, right, zephyr$getGuiTicks());
    }

    @Inject(method = "extractFood", at = @At("RETURN"))
    private void zephyr$appleSkinExtractFoodPost(GuiGraphicsExtractor extractor, Player player, int top, int right, CallbackInfo ci) {
        AppleSkin.INSTANCE.onExtractFoodPost(extractor, player, top, right, zephyr$getGuiTicks());
    }

    @Inject(method = "extractPlayerHealth", at = @At("RETURN"))
    private void zephyr$appleSkinExtractPlayerHealth(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int left = extractor.guiWidth() / 2 - 91;
        int top = extractor.guiHeight() - 39;

        AppleSkin.INSTANCE.onExtractHealth(extractor, player, left, top, zephyr$getGuiTicks());
    }

    private int zephyr$getGuiTicks() {
        return ((Hud) (Object) this).getGuiTicks();
    }
}
