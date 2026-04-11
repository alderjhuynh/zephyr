package com.zephyr.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zephyr.client.disable.*;
import com.zephyr.client.module.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZephyrConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZephyrConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zephyr.json");

    private static boolean suppressSaves;

    private ZephyrConfig() {
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            saveCurrentState();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData config = GSON.fromJson(reader, ConfigData.class);
            if (config == null) {
                LOGGER.warn("Config file was empty, keeping current defaults.");
                saveCurrentState();
                return;
            }

            apply(config);
            saveCurrentState();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load Zephyr config from {}", CONFIG_PATH, exception);
            saveCurrentState();
        }
    }

    public static synchronized void saveCurrentState() {
        if (suppressSaves) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(new ConfigData(), writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save Zephyr config to {}", CONFIG_PATH, exception);
        }
    }

    private static void apply(ConfigData config) {
        suppressSaves = true;
        try {
            Sprint.enabled = config.sprintEnabled;
            AutoRespawn.enabled = config.autoRespawnEnabled;
            AntiHunger.enabled = config.antiHungerEnabled;
            NoFall.enabled = config.noFallEnabled;
            AutoTool.enabled = config.autoToolEnabled;
            GhostHand.enabled = config.ghostHandEnabled;
            disablePortalGuiClosing.enabled = config.portalsEnabled;
            disableDeadMobInteraction.enabled = config.deadMobInteractionEnabled;
            disableAxeStripping.enabled = config.axeStrippingEnabled;
            disableShovelPathing.enabled = config.shovelPathingEnabled;
            disableBlockBreakingCooldown.enabled = config.blockBreakingCooldownEnabled;
            disableBlockBreakingParticles.enabled = config.blockBreakingParticlesEnabled;
            disableInventoryEffectRendering.enabled = config.inventoryEffectRenderingEnabled;
            disableNauseaOverlay.enabled = config.nauseaOverlayEnabled;
            disableNetherPortalSound.enabled = config.netherPortalSoundEnabled;
            disableFogRendering.enabled = config.fogRenderingEnabled;
            disableFirstPersonEffectParticles.enabled = config.firstPersonEffectParticlesEnabled;
            disableRainEffects.enabled = config.rainEffectsEnabled;
            disableDeadMobRendering.enabled = config.deadMobRenderingEnabled;
            TridentBoost.enabled = config.tridentBoostEnabled;
            Blink.CanUseKeybind = config.blinkKeybindEnabled;
            Jesus.enabled = config.jesusEnabled;
            Criticals.enabled = config.criticalsEnabled;
            Criticals.setSpoofHeight(config.criticalsSpoofHeight);
            ElytraBoost.enabled = config.elytraBoostEnabled;
            ElytraBoost.dontConsumeFirework = config.elytraDontConsumeFirework;
            ElytraBoost.fireworkLevel = config.elytraFireworkLevel;
            ElytraBoost.playSound = config.elytraPlaySound;
            ItemRestock.enabled = config.itemRestockEnabled;
            PearlBoost.setBoostVelocity(config.pearlBoostVelocity);
            PearlBoost.enabled = config.pearlBoostEnabled;
            HighJump.setMultiplier(config.highJumpMultiplier);
            HighJump.enabled = config.highJumpEnabled;
            LongJump.setMomentum(config.longJumpMomentum);
            LongJump.setEnabled(config.longJumpEnabled);
            Aerodynamics.setAcceleration(config.aerodynamicsAcceleration);
            Aerodynamics.enabled = config.aerodynamicsEnabled;
            Step.setStepHeight(config.stepHeight);
            Step.setEnabled(config.stepEnabled);
            AirJump.setEnabled(config.airJumpEnabled);
            Flight.setEnabled(config.flightEnabled);
            SpeedMine.setMode(config.getSpeedMineMode());
        } finally {
            suppressSaves = false;
        }
    }

    private static final class ConfigData {
        private boolean sprintEnabled = Sprint.enabled;
        private boolean autoRespawnEnabled = AutoRespawn.enabled;
        private boolean antiHungerEnabled = AntiHunger.enabled;
        private boolean noFallEnabled = NoFall.enabled;
        private boolean autoToolEnabled = AutoTool.enabled;
        private boolean ghostHandEnabled = GhostHand.enabled;
        private boolean portalsEnabled = disablePortalGuiClosing.enabled;
        private boolean deadMobInteractionEnabled = disableDeadMobInteraction.enabled;
        private boolean axeStrippingEnabled = disableAxeStripping.enabled;
        private boolean shovelPathingEnabled = disableShovelPathing.enabled;
        private boolean blockBreakingCooldownEnabled = disableBlockBreakingCooldown.enabled;
        private boolean blockBreakingParticlesEnabled = disableBlockBreakingParticles.enabled;
        private boolean inventoryEffectRenderingEnabled = disableInventoryEffectRendering.enabled;
        private boolean nauseaOverlayEnabled = disableNauseaOverlay.enabled;
        private boolean netherPortalSoundEnabled = disableNetherPortalSound.enabled;
        private boolean fogRenderingEnabled = disableFogRendering.enabled;
        private boolean firstPersonEffectParticlesEnabled = disableFirstPersonEffectParticles.enabled;
        private boolean rainEffectsEnabled = disableRainEffects.enabled;
        private boolean deadMobRenderingEnabled = disableDeadMobRendering.enabled;
        private boolean tridentBoostEnabled = TridentBoost.enabled;
        private boolean blinkKeybindEnabled = Blink.CanUseKeybind;
        private boolean jesusEnabled = Jesus.enabled;
        private boolean criticalsEnabled = Criticals.enabled;
        private double criticalsSpoofHeight = Criticals.getSpoofHeight();
        private boolean elytraBoostEnabled = ElytraBoost.enabled;
        private boolean elytraDontConsumeFirework = ElytraBoost.dontConsumeFirework;
        private int elytraFireworkLevel = ElytraBoost.fireworkLevel;
        private boolean elytraPlaySound = ElytraBoost.playSound;
        private boolean itemRestockEnabled = ItemRestock.enabled;
        private boolean pearlBoostEnabled = PearlBoost.enabled;
        private double pearlBoostVelocity = PearlBoost.getBoostVelocity();
        private boolean highJumpEnabled = HighJump.enabled;
        private double highJumpMultiplier = HighJump.getMultiplier();
        private boolean longJumpEnabled = LongJump.enabled;
        private double longJumpMomentum = LongJump.getMomentum();
        private boolean aerodynamicsEnabled = Aerodynamics.enabled;
        private double aerodynamicsAcceleration = Aerodynamics.getAcceleration();
        private boolean stepEnabled = Step.isEnabled();
        private double stepHeight = Step.getStepHeight();
        private boolean airJumpEnabled = AirJump.enabled;
        private boolean flightEnabled = Flight.enabled;
        private String speedMineMode = SpeedMine.mode.name();

        private SpeedMine.Mode getSpeedMineMode() {
            try {
                return SpeedMine.Mode.valueOf(speedMineMode);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return SpeedMine.Mode.OFF;
            }
        }
    }
}
