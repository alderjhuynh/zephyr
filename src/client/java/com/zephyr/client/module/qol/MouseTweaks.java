package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;

/**
 * QOL inventory tweaks: replaces vanilla RMB dragging, adds LMB drag handling and wheel quick-move.
 *
 * <p>Four tweaks:
 * <ul>
 *   <li>RMB Tweak: like vanilla RMB drag but allows revisiting slots to place again.</li>
 *   <li>LMB Tweak (with item): drag with an item on cursor to pick up matching items; shift-drag to quick-move.</li>
 *   <li>LMB Tweak (without item): shift + LMB drag to quick-move every visited slot.</li>
 *   <li>Wheel Tweak: scroll over a stack to push one item per tick to the other inventory (down) or pull from it (up).</li>
 * </ul>
 */
public final class MouseTweaks extends Module {
    public static final MouseTweaks INSTANCE = new MouseTweaks();

    public enum WheelSearchOrder {
        FIRST_TO_LAST,
        LAST_TO_FIRST
    }

    public enum WheelScrollDirection {
        NORMAL,
        INVERTED
    }

    private final BooleanSetting rmbTweak = new BooleanSetting("RMB Tweak", true);
    private final BooleanSetting lmbTweakWithItem = new BooleanSetting("LMB Tweak With Item", true);
    private final BooleanSetting lmbTweakWithoutItem = new BooleanSetting("LMB Tweak Without Item", true);
    private final BooleanSetting wheelTweak = new BooleanSetting("Wheel Tweak", true);
    private final EnumSetting<WheelSearchOrder> wheelSearchOrder = new EnumSetting<>("Wheel Search Order", WheelSearchOrder.LAST_TO_FIRST);
    private final EnumSetting<WheelScrollDirection> wheelScrollDirection = new EnumSetting<>("Wheel Scroll Direction", WheelScrollDirection.NORMAL);

    private MouseTweaks() {
        super("Mouse Tweaks", "Replaces RMB dragging, adds LMB dragging and scroll wheel quick-move", Category.QOL);
        addSetting(rmbTweak);
        addSetting(lmbTweakWithItem);
        addSetting(lmbTweakWithoutItem);
        addSetting(wheelTweak);
        addSetting(wheelSearchOrder);
        addSetting(wheelScrollDirection);
    }

    public boolean rmbTweakEnabled() {
        return rmbTweak.get();
    }

    public boolean lmbTweakWithItemEnabled() {
        return lmbTweakWithItem.get();
    }

    public boolean lmbTweakWithoutItemEnabled() {
        return lmbTweakWithoutItem.get();
    }

    public boolean wheelTweakEnabled() {
        return wheelTweak.get();
    }

    public WheelSearchOrder wheelSearchOrder() {
        return wheelSearchOrder.get();
    }

    public WheelScrollDirection wheelScrollDirection() {
        return wheelScrollDirection.get();
    }

    public boolean isWheelInverted() {
        return wheelScrollDirection.get() == WheelScrollDirection.INVERTED;
    }
}
