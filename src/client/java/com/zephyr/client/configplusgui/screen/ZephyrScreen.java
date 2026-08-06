package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.hud.PartyManager;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared chrome for every Zephyr menu screen (module list, keybinds, profiles, config):
 * the panel background/border, the "ZEPHYR" title, and the small screen-indicator
 * tag in the bottom right. Also plays the slide transition used when cycling
 * between screens with the "Cycle Screen" keybind (Tab by default - see
 * {@link KeybindManager}). {@link NotificationManager}'s hotkey-toggle popups reuse
 * this same slide/ease-out-cubic timing, just mirrored for a top-right corner toast
 * instead of a full-panel swap.
 * <p>
 * Owns the panel's size and position, recomputed fresh from the window's current
 * (GUI-scale-adjusted) width/height every time {@code init()} runs - which vanilla
 * re-runs on every resize - so the panel always fits on screen instead of the old
 * fixed 300x360 box that used to run off the edge of small windows.
 */
public abstract class ZephyrScreen extends Screen {
    public static final int PANEL_BG = 0xE0141018;
    protected static final int TAB_BG = 0x30FFFFFF;
    protected static final int ROW_BG = 0x40FFFFFF;
    protected static final int ROW_BG_HOVER = 0x60FFFFFF;
    public static final int TEXT_MAIN = 0xFFF2EAFB;
    public static final int TEXT_DIM = 0xFFAFA5C0;
    protected static final int TEXT_ON_ACCENT = 0xFF1B1420;

    /** Solid accent (selected tabs, enabled rows, titles, toasts). Follows the config theme / custom color. */
    public static int accent() {
        return GlobalConfig.accent();
    }

    /** Partially transparent accent, e.g. the click-gui scroll bar. Follows the config theme / custom color. */
    protected static int accentDim() {
        return GlobalConfig.accentDim();
    }

    /** Faint accent wash behind enabled/highlighted rows. Follows the config theme / custom color. */
    protected static int rowBgEnabled() {
        return GlobalConfig.enabledBg();
    }

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
    private static final long BASE_SLIDE_DURATION_NANOS = 180_000_000L;
    /** Floor so a very fast animation speed never reads as a teleport. */
    private static final long MIN_SLIDE_DURATION_NANOS = 40_000_000L;

    protected int panelX;
    protected int panelY;
    protected int panelWidth;
    protected int panelHeight;

    private final int enterDirection;
    private final boolean slideVertically;
    private final long openedAtNanos = System.nanoTime();

    protected enum Nav {
        MAIN {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new ClickGuiScreen(direction, slideVertically);
            }
        },
        KEYBIND {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new KeybindGuiScreen(direction);
            }
        },
        PROFILES {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new ProfileGuiScreen(direction);
            }
        },
        CONFIG {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new ConfigGuiScreen(direction);
            }
        },
        SECRET {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new SecretGuiScreen(direction);
            }

            @Override
            Nav forward() {
                return CREDITS;
            }

            @Override
            Nav backward() {
                return MAIN;
            }
        },
        CREDITS {
            @Override
            Screen create(int direction, boolean slideVertically) {
                return new CreditsGuiScreen(direction);
            }

            @Override
            Nav forward() {
                return MAIN;
            }

            @Override
            Nav backward() {
                return SECRET;
            }
        };

        abstract Screen create(int direction, boolean slideVertically);

        Nav next() {
            Nav[] values = values();
            Nav result = values[(ordinal() + 1) % values.length];
            // The secret and credits screens are hidden easter eggs: the normal Tab
            // cycle skips over them entirely (Tab inside either returns straight to
            // the module list via next() -> MAIN).
            while (result == SECRET || result == CREDITS) {
                result = values[(result.ordinal() + 1) % values.length];
            }
            return result;
        }

        /** Previous screen in the normal cycle, skipping the hidden easter-egg screens. */
        Nav previous() {
            Nav[] values = values();
            Nav result = values[(ordinal() - 1 + values.length) % values.length];
            while (result == SECRET || result == CREDITS) {
                result = values[(result.ordinal() - 1 + values.length) % values.length];
            }
            return result;
        }

        /**
         * Next screen in the hidden three-screen cycle (main -> ??? -> credits). Any
         * regular screen hops straight into the cycle at ???.
         */
        Nav forward() {
            return SECRET;
        }

        /**
         * Previous screen in the hidden three-screen cycle (main -> credits -> ???).
         * Any regular screen hops straight into the cycle at credits.
         */
        Nav backward() {
            return CREDITS;
        }
    }

    protected ZephyrScreen(Component title, int enterDirection) {
        this(title, enterDirection, false);
    }

    protected ZephyrScreen(Component title, int enterDirection, boolean slideVertically) {
        super(title);
        this.enterDirection = enterDirection;
        this.slideVertically = slideVertically;
    }

    protected abstract Nav currentNav();

    public final Screen next() {
        return currentNav().next().create(1, false);
    }

    /**
     * Tab-cycle resolution for {@link KeybindManager}: holding the Down arrow while
     * cycling moves forward through the hidden three-screen cycle (main, ???, credits)
     * and holding Up moves backward through it. The whole cycle slides vertically, so
     * the new screen drops in from the bottom going forward and from the top going
     * backward. Holding Left instead cycles backward through the normal screens (config,
     * profiles, keybinds, main), sliding horizontally like a plain Tab. A plain Tab
     * without arrows keeps the normal screen cycle (main, keybinds, profiles, config),
     * and Tab inside either hidden screen always returns to the module list.
     */
    public final Screen advance(boolean holdingDown, boolean holdingUp, boolean holdingLeft) {
        if (holdingDown) {
            return currentNav().forward().create(1, true);
        }
        if (holdingUp) {
            return currentNav().backward().create(-1, true);
        }
        if (holdingLeft) {
            return currentNav().previous().create(-1, false);
        }
        return next();
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
        int xOffset = slideVertically ? 0 : slideOffsetPx();
        int yOffset = slideVertically ? slideOffsetPx() : 0;
        if (xOffset == 0 && yOffset == 0) {
            renderBody.run();
            return;
        }
        int trueX = panelX;
        int trueY = panelY;
        panelX = trueX + xOffset;
        panelY = trueY + yOffset;
        try {
            renderBody.run();
        } finally {
            panelX = trueX;
            panelY = trueY;
        }
    }

    /** Panel slide length, scaled by the Menu Animation Speed setting. */
    protected static long slideDurationNanos() {
        return Math.max(MIN_SLIDE_DURATION_NANOS,
                (long) (BASE_SLIDE_DURATION_NANOS * GlobalConfig.animationSpeed()));
    }

    private int slideOffsetPx() {
        if (enterDirection == 0) return 0;
        long duration = slideDurationNanos();
        long elapsed = System.nanoTime() - openedAtNanos;
        if (elapsed >= duration) return 0;

        double t = elapsed / (double) duration;
        double eased = 1 - Math.pow(1 - t, 3); // ease-out cubic
        return (int) Math.round((1 - eased) * enterDirection * (panelWidth + SCREEN_MARGIN));
    }

    /** Bottom-right screen indicator; the secret menu replaces it with "???". */
    protected String indicatorText() {
        return currentNav().name();
    }

    protected void renderChrome(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, accent());

        int titleY = panelY + 8;
        if (PartyManager.wobble) {
            titleY += (int) (Math.sin(System.currentTimeMillis() / 120.0) * 2);
        }
        graphics.text(this.font, "ZEPHYR", panelX + PADDING, titleY, accent(), false);

        String indicator = indicatorText();
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