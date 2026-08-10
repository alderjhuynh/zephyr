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

/**
 * Mixin into {@link Hud} that hooks the vanilla HUD food and health extraction
 * methods to feed the Zephyr AppleSkin module's HUD overlays.
 *
 * <p>Injects around {@code Hud.extractFood} (pre to draw the exhaustion
 * underlay, post to draw the saturation / food-value overlays) and at the
 * return of {@code Hud.extractPlayerHealth} to draw the estimated-health
 * overlay. The delegate calls live in
 * {@link com.zephyr.client.module.qol.AppleSkin}.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Called before the vanilla food bar is drawn so the exhaustion underlay can
     * be rendered beneath it.
     *
     * @param extractor the graphics extractor providing HUD layout and draw calls
     * @param player    the local player whose food data is displayed
     * @param top       the y-coordinate of the food bar
     * @param right     the x-coordinate of the right edge of the food bar
     * @param ci        mixin callback info (unused)
     */
    @Inject(method = "extractFood", at = @At("HEAD"))
    private void zephyr$appleSkinExtractFoodPre(GuiGraphicsExtractor extractor, Player player, int top, int right, CallbackInfo ci) {
        AppleSkin.INSTANCE.onExtractFoodPre(extractor, player, top, right, zephyr$getGuiTicks());
    }

    /**
     * Called after the vanilla food bar is drawn so the saturation and
     * food-value overlays can be rendered on top of it.
     *
     * @param extractor the graphics extractor providing HUD layout and draw calls
     * @param player    the local player whose food data is displayed
     * @param top       the y-coordinate of the food bar
     * @param right     the x-coordinate of the right edge of the food bar
     * @param ci        mixin callback info (unused)
     */
    @Inject(method = "extractFood", at = @At("RETURN"))
    private void zephyr$appleSkinExtractFoodPost(GuiGraphicsExtractor extractor, Player player, int top, int right, CallbackInfo ci) {
        AppleSkin.INSTANCE.onExtractFoodPost(extractor, player, top, right, zephyr$getGuiTicks());
    }

    /**
     * Called after the vanilla health bar is drawn so the estimated-health
     * overlay can be rendered on top of it.
     *
     * @param extractor the graphics extractor providing HUD layout and draw calls
     * @param ci        mixin callback info (unused)
     */
    @Inject(method = "extractPlayerHealth", at = @At("RETURN"))
    private void zephyr$appleSkinExtractPlayerHealth(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int left = extractor.guiWidth() / 2 - 91;
        int top = extractor.guiHeight() - 39;

        AppleSkin.INSTANCE.onExtractHealth(extractor, player, left, top, zephyr$getGuiTicks());
    }

    /**
     * Reads the HUD's current GUI tick counter, used to drive the overlay
     * flash animation timing.
     *
     * @return the number of ticks the HUD has been rendering
     */
    private int zephyr$getGuiTicks() {
        return ((Hud) (Object) this).getGuiTicks();
    }
}
