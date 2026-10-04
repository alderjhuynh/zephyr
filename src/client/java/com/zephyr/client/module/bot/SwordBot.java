package com.zephyr.client.module.bot;

import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.bot.sword.BotConfig;
import com.zephyr.client.module.bot.sword.BotEngine;
import net.minecraft.client.Minecraft;

/**
 * Client-side PvP combat bot, ported from swordbot-v3 into Zephyr's module
 * system. Owns a {@link BotEngine} (aim/melee/bow/pursue/recover brains plus
 * targeting, kit doctrine, fight logging and the tiny NN) and maps every
 * {@link BotConfig} field onto a click-gui setting so it persists via
 * {@code modules.json} like any other module.
 *
 * <p>Toggle the module to start/stop the bot. Retargeting, skill/style cycling
 * and NN toggling (swordbot's J/K-style keybinds) are exposed via
 * {@code .z swordbot} instead of raw keybinds.
 */
public final class SwordBot extends Module {
    public static final SwordBot INSTANCE = new SwordBot();

    private final BotEngine engine = new BotEngine();
    private BotConfig.Style lastStyle;

    private final EnumSetting<BotConfig.Skill> skill = new EnumSetting<>("Skill", BotConfig.Skill.OPTIMAL);
    private final EnumSetting<BotConfig.Style> style = new EnumSetting<>("Style", BotConfig.Style.BALANCED);
    private final BooleanSetting allowBow = new BooleanSetting("Allow Bow", true);
    private final BooleanSetting allowEat = new BooleanSetting("Allow Eat", true);
    private final BooleanSetting autoTarget = new BooleanSetting("Auto Target", true);
    private final BooleanSetting nnEnabled = new BooleanSetting("NN Enabled", false);
    private final BooleanSetting cloneDump = new BooleanSetting("Clone Dump", false);
    private final NumberSetting gapHp = new NumberSetting("Gap HP", 12.0, 0.0, 20.0, 0.5);
    private final NumberSetting critHp = new NumberSetting("Crit HP", 7.0, 0.0, 20.0, 0.5);
    private final NumberSetting bowMinDist = new NumberSetting("Bow Min Dist", 13.0, 0.0, 64.0, 0.5);
    private final NumberSetting bowMaxDist = new NumberSetting("Bow Max Dist", 30.0, 0.0, 128.0, 0.5);
    private final NumberSetting punishEaterDist = new NumberSetting("Punish Eater Dist", 6.0, 0.0, 64.0, 0.5);
    private final NumberSetting autoTargetRange = new NumberSetting("Auto Target Range", 100.0, 4.0, 256.0, 1.0);

    private SwordBot() {
        super("SwordBot", "Client-side PvP combat bot (swordbot-v3 port)", Category.BOT);
        addSetting(skill);
        addSetting(style);
        addSetting(allowBow);
        addSetting(allowEat);
        addSetting(autoTarget);
        addSetting(nnEnabled);
        addSetting(cloneDump);
        addSetting(gapHp);
        addSetting(critHp);
        addSetting(bowMinDist);
        addSetting(bowMaxDist);
        addSetting(punishEaterDist);
        addSetting(autoTargetRange);
        lastStyle = style.get();
    }

    /** The wrapped swordbot engine; used by the {@code .z swordbot} command. */
    public BotEngine engine() {
        return engine;
    }

    /** Copies every setting value into the engine config. */
    private void syncConfig() {
        BotConfig config = engine.config();
        config.skill = skill.get();
        BotConfig.Style nextStyle = style.get();
        if (nextStyle != lastStyle) {
            lastStyle = nextStyle;
            config.style = nextStyle;
            engine.noteManualStyle();
        } else {
            config.style = nextStyle;
        }
        config.allowBow = allowBow.get();
        config.allowEat = allowEat.get();
        config.autoTarget = autoTarget.get();
        config.nnEnabled = nnEnabled.get();
        config.cloneDump = cloneDump.get();
        config.gapHpThreshold = gapHp.get();
        config.critHpThreshold = critHp.get();
        config.bowMinDist = bowMinDist.get();
        config.bowMaxDist = bowMaxDist.get();
        config.punishEaterDist = punishEaterDist.get();
        config.autoTargetRange = autoTargetRange.get();
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        syncConfig();
        engine.setEnabled(client, true);
        CommandManager.sendMessage(engine.statusLine());
    }

    @Override
    protected void onDisable() {
        engine.setEnabled(Minecraft.getInstance(), false);
    }

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null) return;
        syncConfig();
        engine.tick(client);
    }

    /** Re-acquires the nearest target, mirroring swordbot's target key. */
    public void retarget(Minecraft client) {
        syncConfig();
        engine.retarget(client);
        CommandManager.sendMessage(engine.statusLine());
    }

    /** Cycles the skill setting, mirroring swordbot's skill key. */
    public void cycleSkill() {
        skill.cycle();
        syncConfig();
        CommandManager.sendMessage(engine.statusLine());
    }

    /** Cycles the style setting, mirroring swordbot's style key. */
    public void cycleStyle() {
        style.cycle();
        syncConfig();
        CommandManager.sendMessage(engine.statusLine());
    }

    /** Toggles the NN setting, mirroring swordbot's nn key. */
    public void toggleNn(Minecraft client) {
        nnEnabled.toggle();
        syncConfig();
        CommandManager.sendMessage(engine.statusLine());
    }
}
