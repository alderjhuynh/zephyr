package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.config.StealthManager;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws the in-game HUD overlay panels driven by {@link GlobalConfig#hudMode()}. Text is
 * resolved through {@link PlaceholderEngine} so lines can embed dynamic values (version,
 * server, FPS, active module count, profile, theme). All panels share the same look: a
 * translucent background, an accent bar along the top, and accent-tinted header text.
 * Rendering is skipped entirely while Stealth Mode is active.
 */
public final class HudRenderer {
    private static final int MARGIN = 4;
    private static final int PADDING = 6;
    private static final int LINE_GAP = 2;
    private static final int ACCENT_BAR_HEIGHT = 2;

    private HudRenderer() {
    }

    /**
     * Renders the HUD overlay for the current {@link HudMode}. No-op for
     * {@link HudMode#OFF} or while {@link StealthManager} is active.
     */
    public static void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight) {
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

    /** Renders the top-left watermark/profile panel for {@link HudMode#MINIMAL}. */
    private static void renderMinimal(GuiGraphicsExtractor graphics, Font font) {
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

    /** Renders the top-right panel listing alphabetically sorted enabled module names. */
    private static void renderModules(GuiGraphicsExtractor graphics, Font font, int screenWidth) {
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

    /** Renders the bottom-left info stack (version, server, FPS, module count, profile, theme). */
    private static void renderInfoStack(GuiGraphicsExtractor graphics, Font font, int screenHeight) {
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

    /** Draws the shared panel background plus the accent bar along its top edge. */
    private static void fillPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + ACCENT_BAR_HEIGHT, accent());
    }

    /** Draws a single text line at the given position. */
    private static void drawLine(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    /** Resolves the FULL-mode info templates, dropping any that render empty. */
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

    /** Collects and case-insensitively sorts the names of all enabled modules. */
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

    /** The widest line's pixel width, used to size the panel. */
    private static int maxLineWidth(Font font, List<String> lines) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    /** Total pixel height of {@code count} lines including their gaps. */
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
