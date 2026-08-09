package com.zephyr.client.module.qol.jade;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.qol.jade.provider.ProviderRegistry;
import net.minecraft.client.Minecraft;

/**
 * A Jade/WTHIT-style "what am I looking at" overlay. The module itself only holds
 * settings; {@link JadeRenderer} does the ray-casting and drawing, and content is
 * produced by registered {@link com.zephyr.client.module.qol.jade.provider.IComponentProvider}s.
 */
public final class Jade extends Module {
    public static final Jade INSTANCE = new Jade();

    private final BooleanSetting showModName = new BooleanSetting("Show Mod Name", true);
    private final BooleanSetting showBlockStates = new BooleanSetting("Show Block States", true);
    private final BooleanSetting showCropProgress = new BooleanSetting("Show Crop Progress", true);
    private final BooleanSetting showRedstone = new BooleanSetting("Show Redstone", true);
    private final BooleanSetting showTntStability = new BooleanSetting("Show TNT Stability", true);
    private final BooleanSetting showBeehive = new BooleanSetting("Show Beehive Level", true);
    private final BooleanSetting showMobHealth = new BooleanSetting("Show Mob Health", true);
    private final BooleanSetting showMobArmor = new BooleanSetting("Show Mob Armor", true);
    private final BooleanSetting showHorseStats = new BooleanSetting("Show Horse Stats", true);
    private final BooleanSetting showItemFrame = new BooleanSetting("Show Item Frame", true);
    private final NumberSetting extendedReach = new NumberSetting("Extended Reach", 0.0, 0.0, 5.0, 0.5);
    private final EnumSetting<OverlayPosition> overlayPosition = new EnumSetting<>("Overlay Position", OverlayPosition.TOP_CENTER);
    private final NumberSetting xOffset = new NumberSetting("X Offset", 0.0, -200.0, 200.0, 1.0);
    private final NumberSetting yOffset = new NumberSetting("Y Offset", 0.0, -200.0, 200.0, 1.0);
    private final BooleanSetting fadeAnimation = new BooleanSetting("Fade Animation", true);
    private final NumberSetting overlayAlpha = new NumberSetting("Overlay Alpha", 1.0, 0.1, 1.0, 0.05);

    private Jade() {
        super("Jade", "Shows a tooltip with details about the block or entity you're looking at", Category.QOL);
        addSetting(showModName);
        addSetting(showBlockStates);
        addSetting(showCropProgress);
        addSetting(showRedstone);
        addSetting(showTntStability);
        addSetting(showBeehive);
        addSetting(showMobHealth);
        addSetting(showMobArmor);
        addSetting(showHorseStats);
        addSetting(showItemFrame);
        addSetting(extendedReach);
        addSetting(overlayPosition);
        addSetting(xOffset);
        addSetting(yOffset);
        addSetting(fadeAnimation);
        addSetting(overlayAlpha);
    }

    @Override
    public void tick(Minecraft client) {
        JadeRenderer.tick(client);
    }

    @Override
    protected void onEnable() {
        ProviderRegistry.init();
    }

    @Override
    protected void onDisable() {
        JadeRenderer.reset();
    }

    public boolean showModName() {
        return showModName.get();
    }

    public boolean showBlockStates() {
        return showBlockStates.get();
    }

    public boolean showCropProgress() {
        return showCropProgress.get();
    }

    public boolean showRedstone() {
        return showRedstone.get();
    }

    public boolean showTntStability() {
        return showTntStability.get();
    }

    public boolean showBeehive() {
        return showBeehive.get();
    }

    public boolean showMobHealth() {
        return showMobHealth.get();
    }

    public boolean showMobArmor() {
        return showMobArmor.get();
    }

    public boolean showHorseStats() {
        return showHorseStats.get();
    }

    public boolean showItemFrame() {
        return showItemFrame.get();
    }

    public double extendedReach() {
        return extendedReach.get();
    }

    public OverlayPosition overlayPosition() {
        return overlayPosition.get();
    }

    public int xOffset() {
        return (int) Math.round(xOffset.get());
    }

    public int yOffset() {
        return (int) Math.round(yOffset.get());
    }

    public boolean fadeAnimation() {
        return fadeAnimation.get();
    }

    public float overlayAlpha() {
        return (float) (double) overlayAlpha.get();
    }

    /** Convenience getters used by the providers. */
    public int borderColor() {
        return JadeColors.NORMAL;
    }

    public int backgroundColor() {
        return JadeColors.BAR_BACKGROUND;
    }
}
