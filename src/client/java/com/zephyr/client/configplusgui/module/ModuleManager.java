package com.zephyr.client.configplusgui.module;

import com.zephyr.client.configplusgui.config.ConfigManager;
import com.zephyr.client.configplusgui.config.ProfileManager;
import com.zephyr.client.module.bot.pathing.Pathing;
import com.zephyr.client.module.bot.SwordBot;
import com.zephyr.client.module.combat.KillAura.*;
import com.zephyr.client.module.combat.AnimeProtagonist.*;

import com.zephyr.client.module.combat.*;
import com.zephyr.client.module.movement.*;
import com.zephyr.client.module.disable.*;
import com.zephyr.client.module.qol.*;
import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.hidden.WhatEvenIsThis;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of every module singleton. {@link #init()} instantiates the movement, disable,
 * qol, combat and bot modules from the {@code com.zephyr.client.module} sub-packages and then
 * restores their persisted state via {@link ConfigManager#load}. The registry drives the
 * click-gui listing, per-tick dispatch of enabled modules, and the enable-count shown in
 * the HUD; the "Default" profile's initial snapshot is built from it via
 * {@link ProfileManager}.
 */
public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    /** Registers every module singleton and loads their persisted state from disk. */
    public static void init() {
        // movement
        register(Aerodynamics.INSTANCE);
        register(AirJump.INSTANCE);
        register(AntiHunger.INSTANCE);
        register(ElytraBoost.INSTANCE);
        register(Flight.INSTANCE);
        register(HighJump.INSTANCE);
        register(NoFall.INSTANCE);
        register(NoSlowdown.INSTANCE);
        register(Sprint.INSTANCE);
        register(Step.INSTANCE);
        register(TridentBoost.INSTANCE);
        register(AutoWalk.INSTANCE);
        register(Blink.INSTANCE);
        register(IceSpeed.INSTANCE);
        register(Jesus.INSTANCE);
        register(Scaffold.INSTANCE);
        // disable
        register(disableAxeStripping.INSTANCE);
        register(disableBlockBreakingCooldown.INSTANCE);
        register(disableBlockOutline.INSTANCE);
        register(disableBlockBreakingParticles.INSTANCE);
        register(disableBossbar.INSTANCE);
        register(disableDeadMobInteraction.INSTANCE);
        register(disableDeadMobRendering.INSTANCE);
        register(disableFirstPersonFire.INSTANCE);
        register(disableFireRendering.INSTANCE);
        register(disableFirstPersonEffectParticles.INSTANCE);
        register(disableFluidFog.INSTANCE);
        register(disableFogRendering.INSTANCE);
        register(disableNauseaOverlay.INSTANCE);
        register(disablePortalGuiClosing.INSTANCE);
        register(disableNetherPortalSound.INSTANCE);
        register(disableRainEffects.INSTANCE);
        register(disableScoreboard.INSTANCE);
        register(disableShovelPathing.INSTANCE);
        register(disableTotemAnimation.INSTANCE);
        register(disableDamageTilt.INSTANCE);
        register(disableExplosionParticles.INSTANCE);
        // qol
        register(AppleSkin.INSTANCE);
        register(ArmorRenderer.INSTANCE);
        register(AutoTool.INSTANCE);
        register(ContainerESP.INSTANCE);
        register(DurabilitySwap.INSTANCE);
        register(FastAttack.INSTANCE);
        register(FastUse.INSTANCE);
        register(FreeCam.INSTANCE);
        register(FullBright.INSTANCE);
        register(GuiMove.INSTANCE);
        register(HoldAttack.INSTANCE);
        register(HoldUse.INSTANCE);
        register(InventoryPackets.INSTANCE);
        register(InventoryRenderer.INSTANCE);
        register(ItemRestock.INSTANCE);
        register(Jade.INSTANCE);
        register(PeriodicAttack.INSTANCE);
        register(PeriodicUse.INSTANCE);
        register(PickBeforePlace.INSTANCE);
        register(PlayerESP.INSTANCE);
        register(PotionSaver.INSTANCE);
        register(RenderInvisibility.INSTANCE);
        register(SafeWalk.INSTANCE);
        register(Seedcracker.INSTANCE);
        register(Sneak.INSTANCE);
        register(SpeedMine.INSTANCE);
        register(ShulkerBoxTooltip.INSTANCE);
        register(TimeChanger.INSTANCE);
        register(Tracer.INSTANCE);
        register(Xray.INSTANCE);
        register(Zoom.INSTANCE);
        register(MouseTweaks.INSTANCE);
        register(AuraMap.INSTANCE);
        // combat
        register(AnimeProtagonist.INSTANCE);
        register(AnchorAura.INSTANCE);
        register(AnchorHelper.INSTANCE);
        register(AutoPlace.INSTANCE);
        register(CrystalHelper.INSTANCE);
        register(BreachSwap.INSTANCE);
        register(Criticals.INSTANCE);
        register(DensitySwap.INSTANCE);
        register(HitAssist.INSTANCE);
        register(InstaCart.INSTANCE);
        register(KillAura.INSTANCE);
        register(Knockback.INSTANCE);
        register(LungeSwap.INSTANCE);
        register(PearlCatch.INSTANCE);
        register(Reach.INSTANCE);
        register(ShieldBreaker.INSTANCE);
        register(AutoCrystal.INSTANCE);
        register(SpearDamage.INSTANCE);
        register(TargetStrafe.INSTANCE);
        register(TotemPopNotifier.INSTANCE);
        register(TriggerBot.INSTANCE);
        register(XBowCart.INSTANCE);
        // bot
        register(Pathing.INSTANCE);
        register(SwordBot.INSTANCE);
        // hidden
        register(WhatEvenIsThis.INSTANCE);

        ConfigManager.load(MODULES);
    }

    /** Adds a module to the registry; called from {@link #init()} in display order. */
    private static void register(Module module) {
        MODULES.add(module);
    }

    /** Returns all registered modules in registration order, read-only. */
    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    /** Returns only the modules visible to the current player/mode (hidden modules filtered when not allowed). */
    public static List<Module> getVisibleModules() {
        return HiddenModules.filterVisible(MODULES);
    }

    /**
     * Returns the modules rendered in the main HUD. Hidden modules are never included here,
     * even when they are unlocked — they are listed on the secret screen instead.
     */
    public static List<Module> getHudModules() {
        List<Module> hud = new ArrayList<>();
        for (Module module : MODULES) {
            if (!module.isHidden()) {
                hud.add(module);
            }
        }
        return Collections.unmodifiableList(hud);
    }

    /**
     * Returns every hidden module. Callers must gate rendering on
     * {@link HiddenModules#shouldShowHidden()}; the secret screen does this.
     */
    public static List<Module> getHiddenModules() {
        List<Module> hidden = new ArrayList<>();
        for (Module module : MODULES) {
            if (module.isHidden()) {
                hidden.add(module);
            }
        }
        return Collections.unmodifiableList(hidden);
    }

    /**
     * Looks up a module by name, case-insensitively.
     *
     * @return the matching module, or {@code null} if none is registered
     */
    public static Module get(String name) {
        for (Module module : MODULES) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    /** The number of currently enabled non-hidden modules, matching what the main HUD lists. */
    public static int enabledCount() {
        int count = 0;
        for (Module module : getHudModules()) {
            if (module.isEnabled()) count++;
        }
        return count;
    }

    /** Forwards the client tick to every enabled module. */
    public static void tick(Minecraft client) {
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                module.tick(client);
            }
        }
    }

    /** Persists all module state to {@code modules.json} and refreshes the active profile snapshot. */
    public static void saveAll() {
        ConfigManager.save(MODULES);
        ProfileManager.captureActiveProfile();
    }
}