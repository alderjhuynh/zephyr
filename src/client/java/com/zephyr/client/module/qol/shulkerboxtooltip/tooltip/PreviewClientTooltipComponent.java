package com.zephyr.client.module.qol.shulkerboxtooltip.tooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewContext;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewProvider;
import com.zephyr.client.module.qol.shulkerboxtooltip.ShulkerBoxTooltipApi;
import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import com.zephyr.client.module.qol.shulkerboxtooltip.render.PreviewRenderer;
import com.zephyr.client.module.qol.shulkerboxtooltip.render.RenderContextImpl;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.jetbrains.annotations.NotNull;

public class PreviewClientTooltipComponent implements ClientTooltipComponent {
    private final PreviewRenderer renderer;

    public PreviewClientTooltipComponent(PreviewTooltipComponent data) {
        PreviewRenderer renderer = data.provider().getRenderer();

        if (renderer == null)
            renderer = PreviewRenderer.getDefaultRendererInstance();
        this.renderer = renderer;
        PreviewProvider provider = data.provider();
        PreviewContext context = data.context();

        renderer.setPreview(context, provider);
        renderer.setPreviewType(ShulkerBoxTooltipApi.getCurrentPreviewType(provider.isFullPreviewAvailable(context)));
    }

    @Override
    public int getHeight(@NotNull Font font) {
        return this.renderer.getHeight() + 4;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return this.renderer.getWidth();
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, int totalWidth, int totalHeight,
                             @NotNull GuiGraphics graphics) {
        var extendedGraphics = (GuiGraphicsExtensions) graphics;
        int mouseX = extendedGraphics.getMouseX();
        int mouseY = extendedGraphics.getMouseY();

        this.renderer.draw(new RenderContextImpl(x, y, totalWidth, this.renderer.getHeight(), graphics, font, mouseX,
                mouseY, x, y));
    }
}
