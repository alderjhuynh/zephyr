package com.zephyr.client.configplusgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared chrome for every Zephyr menu screen (module list, keybinds, profiles):
 * the panel background/border, the "ZEPHYR" title, and the small screen-indicator
 * tag in the bottom right. Also plays the slide transition used when cycling
 * between screens with the "Cycle Screen" keybind (Tab by default - see
 * {@link KeybindManager}).
 * <p>
 * Owns the panel's size and position, recomputed fresh from the window's current
 * (GUI-scale-adjusted) width/height every time {@code init()} runs - which vanilla
 * re-runs on every resize - so the panel always fits on screen instead of the old
 * fixed 300x360 box that used to run off the edge of small windows.
 */
public abstract class ZephyrScreen extends Screen {
    protected static final int ACCENT = 0xFFD1B9EB;
    protected static final int ACCENT_DIM = 0x66D1B9EB;
    protected static final int PANEL_BG = 0xE0141018;
    protected static final int TAB_BG = 0x30FFFFFF;
    protected static final int TAB_BG_SELECTED = 0xFFD1B9EB;
    protected static final int ROW_BG = 0x40FFFFFF;
    protected static final int ROW_BG_HOVER = 0x60FFFFFF;
    protected static final int ROW_BG_ENABLED = 0x40D1B9EB;
    protected static final int TEXT_MAIN = 0xFFF2EAFB;
    protected static final int TEXT_DIM = 0xFFAFA5C0;
    protected static final int TEXT_ON_ACCENT = 0xFF1B1420;

    protected static final int PADDING = 10;
    protected static final int TITLE_HEIGHT = 20;

    /** Panel size Zephyr's screens use when there's room for it. */
    private static final int IDEAL_PANEL_WIDTH = 300;
    private static final int IDEAL_PANEL_HEIGHT = 360;
    /** Below this the panel would be too cramped to use; stop shrinking and let it clip instead. */
    private static final int MIN_PANEL_WIDTH = 220;
    private static final int MIN_PANEL_HEIGHT = 200;
    /** Minimum breathing room kept between the panel and the edge of the window. */
    private static final int SCREEN_MARGIN = 16;

    private static final int INDICATOR_GAP = 6;
    private static final long SLIDE_DURATION_NANOS = 180_000_000L;

    protected int panelX;
    protected int panelY;
    protected int panelWidth;
    protected int panelHeight;

    private final int enterDirection;
    private final long openedAtNanos = System.nanoTime();

    protected enum Nav {
        MAIN {
            @Override
            Screen create(int direction) {
                return new ClickGuiScreen(direction);
            }
        },
        KEYBIND {
            @Override
            Screen create(int direction) {
                return new KeybindGuiScreen(direction);
            }
        },
        PROFILES {
            @Override
            Screen create(int direction) {
                return new ProfileGuiScreen(direction);
            }
        };

        abstract Screen create(int direction);

        Nav next() {
            Nav[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    protected ZephyrScreen(Component title, int enterDirection) {
        super(title);
        this.enterDirection = enterDirection;
    }

    protected abstract Nav currentNav();

    public final Screen next() {
        return currentNav().next().create(1);
    }

    @Override
    protected final void init() {
        calculateLayout();
        initWidgets();
    }

    protected void initWidgets() {
    }

    private void calculateLayout() {
        int maxWidth = Math.max(MIN_PANEL_WIDTH, this.width - SCREEN_MARGIN * 2);
        int maxHeight = Math.max(MIN_PANEL_HEIGHT, this.height - SCREEN_MARGIN * 2);

        panelWidth = Math.min(IDEAL_PANEL_WIDTH, maxWidth);
        panelHeight = Math.min(IDEAL_PANEL_HEIGHT, maxHeight);

        panelX = Math.max(0, (this.width - panelWidth) / 2);
        panelY = Math.max(0, (this.height - panelHeight) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected final void withPanelSlide(Runnable renderBody) {
        int offset = slideOffsetPx();
        if (offset == 0) {
            renderBody.run();
            return;
        }
        int trueX = panelX;
        panelX = trueX + offset;
        try {
            renderBody.run();
        } finally {
            panelX = trueX;
        }
    }

    private int slideOffsetPx() {
        if (enterDirection == 0) return 0;
        long elapsed = System.nanoTime() - openedAtNanos;
        if (elapsed >= SLIDE_DURATION_NANOS) return 0;

        double t = elapsed / (double) SLIDE_DURATION_NANOS;
        double eased = 1 - Math.pow(1 - t, 3); // ease-out cubic
        return (int) Math.round((1 - eased) * enterDirection * (panelWidth + SCREEN_MARGIN));
    }

    protected void renderChrome(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, ACCENT);
        graphics.text(this.font, "ZEPHYR", panelX + PADDING, panelY + 8, ACCENT, false);

        String indicator = currentNav().name();
        int indicatorWidth = this.font.width(indicator);
        int indicatorX = panelX + panelWidth - indicatorWidth;
        int indicatorY = panelY + panelHeight + INDICATOR_GAP;
        graphics.text(this.font, indicator, indicatorX, indicatorY, TEXT_DIM, false);
    }

    protected int headerHeight() {
        return TITLE_HEIGHT;
    }

    @Override
    public void onClose() {
        ModuleManager.saveAll();
        super.onClose();
    }
}