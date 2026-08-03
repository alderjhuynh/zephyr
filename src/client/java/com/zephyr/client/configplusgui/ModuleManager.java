package com.zephyr.client.configplusgui;

import com.zephyr.client.module.combat.*;
import com.zephyr.client.module.combat.AnimeProtagonist.AnimeProtagonist;
import com.zephyr.client.module.combat.KillAura.KillAura;
import com.zephyr.client.module.movement.Aerodynamics;
import com.zephyr.client.module.movement.AirJump;
import com.zephyr.client.module.movement.AntiHunger;
import com.zephyr.client.module.movement.ElytraBoost;
import com.zephyr.client.module.movement.Flight;
import com.zephyr.client.module.movement.HighJump;
import com.zephyr.client.module.movement.NoFall;
import com.zephyr.client.module.movement.Sprint;
import com.zephyr.client.module.movement.Step;
import com.zephyr.client.module.movement.TridentBoost;
import com.zephyr.client.module.disable.*;
import com.zephyr.client.module.qol.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.Potion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers every module and owns the master list the click-gui and config loader
 * both read from. Call {@link #init()} once from {@code ZephyrClient.onInitializeClient()}.
 */
public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    /**
     * To add a new module: write your {@link Module} subclass with a public static
     * {@code INSTANCE}, then add one {@code register(...)} line below. It will
     * automatically show up in the click-gui (tab + search), get its enabled
     * state and settings persisted to disk, and get ticked while enabled.
     */
    public static void init() {
        register(Aerodynamics.INSTANCE);
        register(AirJump.INSTANCE);
        register(AntiHunger.INSTANCE);
        register(ElytraBoost.INSTANCE);
        register(Flight.INSTANCE);
        register(HighJump.INSTANCE);
        register(NoFall.INSTANCE);
        register(Sprint.INSTANCE);
        register(Step.INSTANCE);
        register(TridentBoost.INSTANCE);
        register(disableAxeStripping.INSTANCE);
        register(disableBlockBreakingCooldown.INSTANCE);
        register(disableBlockBreakingParticles.INSTANCE);
        register(disableDeadMobInteraction.INSTANCE);
        register(disableDeadMobRendering.INSTANCE);
        register(disableFirstPersonEffectParticles.INSTANCE);
        register(disableFogRendering.INSTANCE);
        register(disableInventoryEffectRendering.INSTANCE);
        register(disableNauseaOverlay.INSTANCE);
        register(disableNetherPortalSound.INSTANCE);
        register(disablePortalGuiClosing.INSTANCE);
        register(disableRainEffects.INSTANCE);
        register(disableShovelPathing.INSTANCE);
        register(Sneak.INSTANCE);
        register(RenderInvisibility.INSTANCE);
        register(SpeedMine.INSTANCE);
        register(PickBeforePlace.INSTANCE);
        register(PeriodicUse.INSTANCE);
        register(PeriodicAttack.INSTANCE);
        register(ItemRestock.INSTANCE);
        register(HoldUse.INSTANCE);
        register(HoldAttack.INSTANCE);
        register(GuiMove.INSTANCE);
        register(FastUse.INSTANCE);
        register(FastAttack.INSTANCE);
        register(DurabilitySwap.INSTANCE);
        register(AutoTool.INSTANCE);
        register(ShieldBreaker.INSTANCE);
        register(BreachSwap.INSTANCE);
        register(Criticals.INSTANCE);
        register(LungeSwap.INSTANCE);
        register(Reach.INSTANCE);
        register(PlayerESP.INSTANCE);
        register(Xray.INSTANCE);
        register(KillAura.INSTANCE);
        register(AnimeProtagonist.INSTANCE);
        register(FullBright.INSTANCE);
        register(DensitySwap.INSTANCE);
        register(AutoPlace.INSTANCE);
        register(KillWyvern.INSTANCE);
        register(PotionSaver.INSTANCE);
        register(DiscordPresence.INSTANCE);

        ConfigManager.load(MODULES);
    }

    private static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static void tick(Minecraft client) {
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                module.tick(client);
            }
        }
    }

    public static void saveAll() {
        ConfigManager.save(MODULES);
    }
}