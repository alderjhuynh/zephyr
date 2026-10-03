package com.zephyr.client.configplusgui.module;

import com.zephyr.client.configplusgui.config.ConfigManager;
import com.zephyr.client.configplusgui.config.ProfileManager;
import com.zephyr.client.module.combat.KillAura.*;
import com.zephyr.client.module.combat.AnimeProtagonist.*;

import com.zephyr.client.module.combat.*;
import com.zephyr.client.module.movement.*;
import com.zephyr.client.module.disable.*;
import com.zephyr.client.module.qol.*;
import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.bots.pathing.Pathing;
import com.zephyr.client.module.hidden.WhatEvenIsThis;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

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
        register(Pathing.INSTANCE);
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
        register(MouseTweaks.INSTANCE);
        register(PeriodicAttack.INSTANCE);
        register(PeriodicUse.INSTANCE);
        register(PickBeforePlace.INSTANCE);
        register(PlayerESP.INSTANCE);
        register(PotionSaver.INSTANCE);
        register(RenderInvisibility.INSTANCE);
        register(SafeWalk.INSTANCE);
        register(Sneak.INSTANCE);
        register(SpeedMine.INSTANCE);
        register(ShulkerBoxTooltip.INSTANCE);
        register(TimeChanger.INSTANCE);
        register(Tracer.INSTANCE);
        register(Xray.INSTANCE);
        register(Zoom.INSTANCE);
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
        register(PearlCatch.INSTANCE);
        register(Reach.INSTANCE);
        register(ShieldBreaker.INSTANCE);
        register(AutoCrystal.INSTANCE);
        register(TargetStrafe.INSTANCE);
        register(TotemPopNotifier.INSTANCE);
        register(TriggerBot.INSTANCE);
        register(XBowCart.INSTANCE);
        // hidden
        register(WhatEvenIsThis.INSTANCE);

        ConfigManager.load(MODULES);
    }

    private static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> getVisibleModules() {
        return HiddenModules.filterVisible(MODULES);
    }

    public static List<Module> getHudModules() {
        List<Module> hud = new ArrayList<>();
        for (Module module : MODULES) {
            if (!module.isHidden()) {
                hud.add(module);
            }
        }
        return Collections.unmodifiableList(hud);
    }

    public static List<Module> getHiddenModules() {
        List<Module> hidden = new ArrayList<>();
        for (Module module : MODULES) {
            if (module.isHidden()) {
                hidden.add(module);
            }
        }
        return Collections.unmodifiableList(hidden);
    }

    public static Module get(String name) {
        for (Module module : MODULES) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static int enabledCount() {
        int count = 0;
        for (Module module : getHudModules()) {
            if (module.isEnabled()) count++;
        }
        return count;
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
        ProfileManager.captureActiveProfile();
    }
}