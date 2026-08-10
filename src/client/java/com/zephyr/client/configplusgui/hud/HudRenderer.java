package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.config.StealthManager;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HudRenderer {
    private static final int MARGIN = 4;
    private static final int PADDING = 6;
    private static final int LINE_GAP = 2;
    private static final int ACCENT_BAR_HEIGHT = 2;

    private HudRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        HudMode mode = GlobalConfig.hudMode();
        if (mode == HudMode.OFF) return;
        if (StealthManager.isActive()) return;

        switch (mode) {
            case MINIMAL -> renderMinimal(graphics, font);
            case FULL -> {
                renderModules(graphics, font, screenWidth);
                renderInfoStack(graphics, font, screenHeight);
            }
            default -> {
            }
        }
    }

    private static void renderMinimal(GuiGraphics graphics, Font font) {
        List<String> lines = new ArrayList<>();
        String watermark = PlaceholderEngine.replace("Zephyr %version%");
        String profile = PlaceholderEngine.replace("%profile%");
        if (!watermark.isEmpty()) lines.add(watermark);
        if (!profile.isEmpty()) lines.add(profile);
        if (lines.isEmpty()) return;

        int panelWidth = maxLineWidth(font, lines) + PADDING * 2;
        int panelHeight = linesHeight(font, lines.size()) + PADDING * 2 + ACCENT_BAR_HEIGHT;
        int x = MARGIN;
        int y = MARGIN;

        fillPanel(graphics, x, y, panelWidth, panelHeight);

        int textX = x + PADDING;
        int textY = y + ACCENT_BAR_HEIGHT + PADDING;
        for (int i = 0; i < lines.size(); i++) {
            drawLine(graphics, font, lines.get(i), textX, textY, i == 0 ? accent() : TEXT_DIM);
            textY += font.lineHeight + LINE_GAP;
        }
    }

    private static void renderModules(GuiGraphics graphics, Font font, int screenWidth) {
        List<String> modules = activeModuleNames();
        if (modules.isEmpty()) return;

        int panelWidth = maxLineWidth(font, modules) + PADDING * 2;
        int panelHeight = linesHeight(font, modules.size()) + PADDING * 2 + ACCENT_BAR_HEIGHT;
        int x = screenWidth - MARGIN - panelWidth;
        int y = MARGIN;

        fillPanel(graphics, x, y, panelWidth, panelHeight);

        int textX = x + PADDING;
        int textY = y + ACCENT_BAR_HEIGHT + PADDING;
        for (String module : modules) {
            drawLine(graphics, font, module, textX, textY, accent());
            textY += font.lineHeight + LINE_GAP;
        }
    }

    private static void renderInfoStack(GuiGraphics graphics, Font font, int screenHeight) {
        String[] stack = nonEmptyLines();
        if (stack.length == 0) return;

        List<String> lines = Arrays.asList(stack);
        int panelWidth = maxLineWidth(font, lines) + PADDING * 2;
        int panelHeight = linesHeight(font, stack.length) + PADDING * 2 + ACCENT_BAR_HEIGHT;
        int x = MARGIN;
        int y = screenHeight - MARGIN - panelHeight;

        fillPanel(graphics, x, y, panelWidth, panelHeight);

        int textX = x + PADDING;
        int textY = y + ACCENT_BAR_HEIGHT + PADDING;
        for (int i = 0; i < stack.length; i++) {
            drawLine(graphics, font, stack[i], textX, textY, i == 0 ? accent() : TEXT_MAIN);
            textY += font.lineHeight + LINE_GAP;
        }
    }

    private static void fillPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + ACCENT_BAR_HEIGHT, accent());
    }

    private static void drawLine(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private static String[] nonEmptyLines() {
        String[] templates = {
                "Zephyr %version%",
                "%server%",
                "%fps% fps",
                "%modules_on% of %modules%",
                "%profile%",
                "%theme%"
        };
        return Arrays.stream(templates)
                .map(PlaceholderEngine::replace)
                .filter(text -> !text.isEmpty())
                .toArray(String[]::new);
    }

    private static List<String> activeModuleNames() {
        List<String> names = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isEnabled()) {
                names.add(module.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static int maxLineWidth(Font font, List<String> lines) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    private static int linesHeight(Font font, int count) {
        return count * font.lineHeight + Math.max(0, count - 1) * LINE_GAP;
    }

    private static int accent() {
        return GlobalConfig.accent();
    }

    private static final int PANEL_BG = 0xE0141018;
    private static final int TEXT_MAIN = 0xFFF2EAFB;
    private static final int TEXT_DIM = 0xFFAFA5C0;
}
