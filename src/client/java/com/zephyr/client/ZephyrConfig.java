package com.zephyr.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zephyr.client.disable.*;
import com.zephyr.client.keybind.ZephyrKeybindManager;
import com.zephyr.client.module.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ZephyrConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZephyrConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir().resolve("zephyr");
    private static final Path PROFILES_DIR = CONFIG_ROOT.resolve("profiles");
    private static final Path MANIFEST_PATH = CONFIG_ROOT.resolve("profiles.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zephyr.json");
    private static final String DEFAULT_PROFILE_ID = "default";
    private static final String DEFAULT_PROFILE_NAME = "Default";
    private static final int MAX_PROFILE_NAME_LENGTH = 32;
    private static final Pattern INVALID_ID_CHARS = Pattern.compile("[^a-z0-9]+");

    private static boolean suppressSaves;
    private static ManifestData manifest;

    private ZephyrConfig() {
    }

    public static synchronized void load() {
        manifest = loadManifest();
        migrateLegacyConfigIfNeeded();
        ensureDefaultProfile();
        ensureSelectedProfile();
        saveManifest();
        loadSelectedProfileState();
    }

    public static synchronized void saveCurrentState() {
        if (suppressSaves) {
            return;
        }

        ensureLoaded();
        writeConfig(getSelectedProfilePath(), ConfigData.captureCurrentState());
    }

    public static synchronized List<ProfileSummary> getProfiles() {
        ensureLoaded();
        String selectedProfileId = manifest.selectedProfileId;

        return manifest.profiles.stream()
                .sorted(Comparator.comparing(ProfileData::name, String.CASE_INSENSITIVE_ORDER))
                .map(profile -> new ProfileSummary(profile.id, profile.name, profile.id.equals(selectedProfileId)))
                .toList();
    }

    public static synchronized String getSelectedProfileName() {
        ensureLoaded();
        return findProfile(manifest.selectedProfileId)
                .map(ProfileData::name)
                .orElse(DEFAULT_PROFILE_NAME);
    }

    public static synchronized void createProfile(String rawName) {
        ensureLoaded();

        String name = validateProfileName(rawName);
        if (manifest.profiles.stream().anyMatch(profile -> profile.name.equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("A profile with that name already exists.");
        }

        saveCurrentState();

        ProfileData profile = new ProfileData(generateProfileId(name), name);
        manifest.profiles.add(profile);
        writeConfig(getProfilePath(profile.id), ConfigData.captureCurrentState());
        manifest.selectedProfileId = profile.id;
        saveManifest();
        loadSelectedProfileState();
    }

    public static synchronized void deleteProfile(String profileId) {
        ensureLoaded();

        ProfileData profile = findProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("That profile does not exist."));
        if (profile.id.equals(manifest.selectedProfileId)) {
            throw new IllegalArgumentException("Select a different profile before deleting this one.");
        }

        manifest.profiles.remove(profile);
        try {
            Files.deleteIfExists(getProfilePath(profile.id));
        } catch (IOException exception) {
            LOGGER.error("Failed to delete Zephyr profile config {}", getProfilePath(profile.id), exception);
        }

        ensureDefaultProfile();
        ensureSelectedProfile();
        saveManifest();
    }

    public static synchronized void selectProfile(String profileId) {
        ensureLoaded();

        ProfileData profile = findProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("That profile does not exist."));
        if (profile.id.equals(manifest.selectedProfileId)) {
            return;
        }

        saveCurrentState();
        manifest.selectedProfileId = profile.id;
        saveManifest();
        loadSelectedProfileState();
    }

    private static void ensureLoaded() {
        if (manifest == null) {
            load();
        }
    }

    private static ManifestData loadManifest() {
        try {
            Files.createDirectories(PROFILES_DIR);
        } catch (IOException exception) {
            LOGGER.error("Failed to create Zephyr config directory {}", PROFILES_DIR, exception);
        }

        if (!Files.exists(MANIFEST_PATH)) {
            return new ManifestData();
        }

        try (Reader reader = Files.newBufferedReader(MANIFEST_PATH)) {
            ManifestData loadedManifest = GSON.fromJson(reader, ManifestData.class);
            if (loadedManifest == null) {
                return new ManifestData();
            }

            loadedManifest.sanitize();
            return loadedManifest;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load Zephyr profile manifest from {}", MANIFEST_PATH, exception);
            return new ManifestData();
        }
    }

    private static void migrateLegacyConfigIfNeeded() {
        if (!Files.exists(LEGACY_CONFIG_PATH)) {
            return;
        }

        Path defaultProfilePath = getProfilePath(DEFAULT_PROFILE_ID);
        if (Files.exists(defaultProfilePath)) {
            return;
        }

        try {
            Files.createDirectories(PROFILES_DIR);
            Files.move(LEGACY_CONFIG_PATH, defaultProfilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.error("Failed to migrate Zephyr legacy config from {}", LEGACY_CONFIG_PATH, exception);
        }
    }

    private static void ensureDefaultProfile() {
        if (findProfile(DEFAULT_PROFILE_ID).isPresent()) {
            return;
        }

        manifest.profiles.addFirst(new ProfileData(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME));
    }

    private static void ensureSelectedProfile() {
        if (findProfile(manifest.selectedProfileId).isPresent()) {
            return;
        }

        manifest.selectedProfileId = manifest.profiles.getFirst().id;
    }

    private static void loadSelectedProfileState() {
        ConfigData config = readConfig(getSelectedProfilePath());
        if (config == null) {
            config = new ConfigData();
        }

        apply(config);
        saveCurrentState();
    }

    private static ConfigData readConfig(Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigData config = GSON.fromJson(reader, ConfigData.class);
            return config == null ? new ConfigData() : config;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load Zephyr profile config from {}", path, exception);
            return new ConfigData();
        }
    }

    private static void writeConfig(Path path, ConfigData config) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save Zephyr profile config to {}", path, exception);
        }
    }

    private static void saveManifest() {
        try {
            Files.createDirectories(MANIFEST_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(MANIFEST_PATH)) {
                GSON.toJson(manifest, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save Zephyr profile manifest to {}", MANIFEST_PATH, exception);
        }
    }

    private static Optional<ProfileData> findProfile(String profileId) {
        return manifest.profiles.stream()
                .filter(profile -> Objects.equals(profile.id, profileId))
                .findFirst();
    }

    private static Path getSelectedProfilePath() {
        return getProfilePath(manifest.selectedProfileId);
    }

    private static Path getProfilePath(String profileId) {
        return PROFILES_DIR.resolve(profileId + ".json");
    }

    private static String validateProfileName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Profile names cannot be empty.");
        }
        if (name.length() > MAX_PROFILE_NAME_LENGTH) {
            throw new IllegalArgumentException("Profile names must be 32 characters or fewer.");
        }

        return name;
    }

    private static String generateProfileId(String name) {
        String normalized = INVALID_ID_CHARS.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        if (normalized.isEmpty()) {
            normalized = "profile";
        }

        Set<String> usedIds = new LinkedHashSet<>();
        for (ProfileData profile : manifest.profiles) {
            usedIds.add(profile.id);
        }

        String candidate = normalized;
        int suffix = 2;
        while (usedIds.contains(candidate)) {
            candidate = normalized + "-" + suffix;
            suffix++;
        }

        return candidate;
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
            ZephyrKeybindManager.setModifierKeyCode(config.keybindModifierKey);
            ZephyrKeybindManager.loadSpecificKeyCodes(config.keybindSpecificKeys);
            Jesus.enabled = config.jesusEnabled;
            Criticals.enabled = config.criticalsEnabled;
            Criticals.setSpoofHeight(config.criticalsSpoofHeight);
            DurabilitySwap.enabled = config.durabilitySwapEnabled;
            ElytraBoost.enabled = config.elytraBoostEnabled;
            ElytraBoost.dontConsumeFirework = config.elytraDontConsumeFirework;
            ElytraBoost.fireworkLevel = config.elytraFireworkLevel;
            ElytraBoost.playSound = config.elytraPlaySound;
            ElytraSwap.enabled = config.elytraSwapEnabled;
            F5Tweaks.enabled = config.f5TweaksEnabled;
            FastAttack.enabled = config.fastAttackEnabled;
            FastAttack.setTimesPerTick(config.fastAttackTimesPerTick);
            ShieldBreaker.enabled = config.attributeSwapEnabled;
            FastUse.enabled = config.fastUseEnabled;
            FastUse.setTimesPerTick(config.fastUseTimesPerTick);
            GuiMove.enabled = config.guiMoveEnabled;
            HoldAttack.enabled = config.holdAttackEnabled;
            HoldUse.enabled = config.holdUseEnabled;
            ItemRestock.enabled = config.itemRestockEnabled;
            HotbarRowSwap.enabled = config.hotbarRowSwapEnabled;
            PearlBoost.setBoostVelocity(config.pearlBoostVelocity);
            PearlBoost.enabled = config.pearlBoostEnabled;
            PeriodicAttack.enabled = config.periodicAttackEnabled;
            PeriodicAttack.setDelayTicks(config.periodicAttackDelayTicks);
            PeriodicUse.enabled = config.periodicUseEnabled;
            PeriodicUse.setDelayTicks(config.periodicUseDelayTicks);
            PickBeforePlace.enabled = config.pickBeforePlaceEnabled;
            RenderInvisibility.enabled = config.renderInvisibilityEnabled;
            HighJump.setMultiplier(config.highJumpMultiplier);
            HighJump.enabled = config.highJumpEnabled;
            LongJump.setMomentum(config.longJumpMomentum);
            LongJump.setEnabled(config.longJumpEnabled);
            Aerodynamics.setAcceleration(config.aerodynamicsAcceleration);
            Aerodynamics.enabled = config.aerodynamicsEnabled;
            Sneak.enabled = config.sneakEnabled;
            Step.setStepHeight(config.stepHeight);
            Step.setEnabled(config.stepEnabled);
            AirJump.setEnabled(config.airJumpEnabled);
            Flight.setEnabled(config.flightEnabled);
            FreeCam.setEnabled(config.freeCamEnabled);
            SpeedMine.setMode(config.getSpeedMineMode());
            BreachSwap.enabled = config.BreachSwapEnabled;
            PearlCatch.enabled = config.PearlCatchEnabled;
        } finally {
            suppressSaves = false;
        }
    }

    public record ProfileSummary(String id, String name, boolean selected) {
    }

    private static final class ManifestData {
        private String selectedProfileId = DEFAULT_PROFILE_ID;
        private List<ProfileData> profiles = new ArrayList<>();

        private void sanitize() {
            if (profiles == null) {
                profiles = new ArrayList<>();
            }

            List<ProfileData> sanitizedProfiles = new ArrayList<>();
            Set<String> ids = new LinkedHashSet<>();
            for (ProfileData profile : profiles) {
                if (profile == null || profile.id == null || profile.id.isBlank() || profile.name == null || profile.name.isBlank()) {
                    continue;
                }
                if (ids.add(profile.id)) {
                    sanitizedProfiles.add(profile);
                }
            }
            profiles = sanitizedProfiles;

            if (selectedProfileId == null || selectedProfileId.isBlank()) {
                selectedProfileId = DEFAULT_PROFILE_ID;
            }
        }
    }

    private record ProfileData(String id, String name) {
    }

    private static final class Defaults {
        private static final boolean SPRINT_ENABLED = Sprint.enabled;
        private static final boolean AUTO_RESPAWN_ENABLED = AutoRespawn.enabled;
        private static final boolean ANTI_HUNGER_ENABLED = AntiHunger.enabled;
        private static final boolean NO_FALL_ENABLED = NoFall.enabled;
        private static final boolean AUTO_TOOL_ENABLED = AutoTool.enabled;
        private static final boolean GHOST_HAND_ENABLED = GhostHand.enabled;
        private static final boolean PORTALS_ENABLED = disablePortalGuiClosing.enabled;
        private static final boolean DEAD_MOB_INTERACTION_ENABLED = disableDeadMobInteraction.enabled;
        private static final boolean AXE_STRIPPING_ENABLED = disableAxeStripping.enabled;
        private static final boolean SHOVEL_PATHING_ENABLED = disableShovelPathing.enabled;
        private static final boolean BLOCK_BREAKING_COOLDOWN_ENABLED = disableBlockBreakingCooldown.enabled;
        private static final boolean BLOCK_BREAKING_PARTICLES_ENABLED = disableBlockBreakingParticles.enabled;
        private static final boolean INVENTORY_EFFECT_RENDERING_ENABLED = disableInventoryEffectRendering.enabled;
        private static final boolean NAUSEA_OVERLAY_ENABLED = disableNauseaOverlay.enabled;
        private static final boolean NETHER_PORTAL_SOUND_ENABLED = disableNetherPortalSound.enabled;
        private static final boolean FOG_RENDERING_ENABLED = disableFogRendering.enabled;
        private static final boolean FIRST_PERSON_EFFECT_PARTICLES_ENABLED = disableFirstPersonEffectParticles.enabled;
        private static final boolean RAIN_EFFECTS_ENABLED = disableRainEffects.enabled;
        private static final boolean DEAD_MOB_RENDERING_ENABLED = disableDeadMobRendering.enabled;
        private static final boolean TRIDENT_BOOST_ENABLED = TridentBoost.enabled;
        private static final boolean BLINK_KEYBIND_ENABLED = Blink.CanUseKeybind;
        private static final int KEYBIND_MODIFIER_KEY = ZephyrKeybindManager.getModifierKeyCode();
        private static final Map<String, Integer> KEYBIND_SPECIFIC_KEYS = ZephyrKeybindManager.getSpecificKeyCodes();
        private static final boolean JESUS_ENABLED = Jesus.enabled;
        private static final boolean CRITICALS_ENABLED = Criticals.enabled;
        private static final double CRITICALS_SPOOF_HEIGHT = Criticals.getSpoofHeight();
        private static final boolean DURABILITY_SWAP_ENABLED = DurabilitySwap.enabled;
        private static final boolean ELYTRA_BOOST_ENABLED = ElytraBoost.enabled;
        private static final boolean ELYTRA_DONT_CONSUME_FIREWORK = ElytraBoost.dontConsumeFirework;
        private static final int ELYTRA_FIREWORK_LEVEL = ElytraBoost.fireworkLevel;
        private static final boolean ELYTRA_PLAY_SOUND = ElytraBoost.playSound;
        private static final boolean ELYTRA_SWAP_ENABLED = ElytraSwap.enabled;
        private static final boolean F5_TWEAKS_ENABLED = F5Tweaks.enabled;
        private static final boolean FAST_ATTACK_ENABLED = FastAttack.enabled;
        private static final int FAST_ATTACK_TIMES_PER_TICK = FastAttack.getTimesPerTick();
        private static final boolean ATTRIBUTE_SWAP_ENABLED = ShieldBreaker.enabled;
        private static final boolean FAST_USE_ENABLED = FastUse.enabled;
        private static final int FAST_USE_TIMES_PER_TICK = FastUse.getTimesPerTick();
        private static final boolean GUI_MOVE_ENABLED = GuiMove.enabled;
        private static final boolean HOLD_ATTACK_ENABLED = HoldAttack.enabled;
        private static final boolean HOLD_USE_ENABLED = HoldUse.enabled;
        private static final boolean ITEM_RESTOCK_ENABLED = ItemRestock.enabled;
        private static final boolean HOTBAR_ROW_SWAP_ENABLED = HotbarRowSwap.enabled;
        private static final boolean PEARL_BOOST_ENABLED = PearlBoost.enabled;
        private static final double PEARL_BOOST_VELOCITY = PearlBoost.getBoostVelocity();
        private static final boolean PERIODIC_ATTACK_ENABLED = PeriodicAttack.enabled;
        private static final int PERIODIC_ATTACK_DELAY_TICKS = PeriodicAttack.getDelayTicks();
        private static final boolean PERIODIC_USE_ENABLED = PeriodicUse.enabled;
        private static final int PERIODIC_USE_DELAY_TICKS = PeriodicUse.getDelayTicks();
        private static final boolean PICK_BEFORE_PLACE_ENABLED = PickBeforePlace.enabled;
        private static final boolean RENDER_INVISIBILITY_ENABLED = RenderInvisibility.enabled;
        private static final boolean HIGH_JUMP_ENABLED = HighJump.enabled;
        private static final double HIGH_JUMP_MULTIPLIER = HighJump.getMultiplier();
        private static final boolean LONG_JUMP_ENABLED = LongJump.enabled;
        private static final double LONG_JUMP_MOMENTUM = LongJump.getMomentum();
        private static final boolean AERODYNAMICS_ENABLED = Aerodynamics.enabled;
        private static final double AERODYNAMICS_ACCELERATION = Aerodynamics.getAcceleration();
        private static final boolean SNEAK_ENABLED = Sneak.enabled;
        private static final boolean STEP_ENABLED = Step.isEnabled();
        private static final double STEP_HEIGHT = Step.getStepHeight();
        private static final boolean AIR_JUMP_ENABLED = AirJump.enabled;
        private static final boolean FLIGHT_ENABLED = Flight.enabled;
        private static final boolean FREE_CAM_ENABLED = FreeCam.enabled;
        private static final String SPEED_MINE_MODE = SpeedMine.mode.name();
        private static final boolean BREACH_SWAP_ENABLED = BreachSwap.enabled;
        private static final boolean PEARL_CATCH_ENABLED = PearlCatch.enabled;
    }

    private static final class ConfigData {
        private boolean sprintEnabled = Defaults.SPRINT_ENABLED;
        private boolean autoRespawnEnabled = Defaults.AUTO_RESPAWN_ENABLED;
        private boolean antiHungerEnabled = Defaults.ANTI_HUNGER_ENABLED;
        private boolean noFallEnabled = Defaults.NO_FALL_ENABLED;
        private boolean autoToolEnabled = Defaults.AUTO_TOOL_ENABLED;
        private boolean ghostHandEnabled = Defaults.GHOST_HAND_ENABLED;
        private boolean portalsEnabled = Defaults.PORTALS_ENABLED;
        private boolean deadMobInteractionEnabled = Defaults.DEAD_MOB_INTERACTION_ENABLED;
        private boolean axeStrippingEnabled = Defaults.AXE_STRIPPING_ENABLED;
        private boolean shovelPathingEnabled = Defaults.SHOVEL_PATHING_ENABLED;
        private boolean blockBreakingCooldownEnabled = Defaults.BLOCK_BREAKING_COOLDOWN_ENABLED;
        private boolean blockBreakingParticlesEnabled = Defaults.BLOCK_BREAKING_PARTICLES_ENABLED;
        private boolean inventoryEffectRenderingEnabled = Defaults.INVENTORY_EFFECT_RENDERING_ENABLED;
        private boolean nauseaOverlayEnabled = Defaults.NAUSEA_OVERLAY_ENABLED;
        private boolean netherPortalSoundEnabled = Defaults.NETHER_PORTAL_SOUND_ENABLED;
        private boolean fogRenderingEnabled = Defaults.FOG_RENDERING_ENABLED;
        private boolean firstPersonEffectParticlesEnabled = Defaults.FIRST_PERSON_EFFECT_PARTICLES_ENABLED;
        private boolean rainEffectsEnabled = Defaults.RAIN_EFFECTS_ENABLED;
        private boolean deadMobRenderingEnabled = Defaults.DEAD_MOB_RENDERING_ENABLED;
        private boolean tridentBoostEnabled = Defaults.TRIDENT_BOOST_ENABLED;
        private boolean blinkKeybindEnabled = Defaults.BLINK_KEYBIND_ENABLED;
        private int keybindModifierKey = Defaults.KEYBIND_MODIFIER_KEY;
        private Map<String, Integer> keybindSpecificKeys = Defaults.KEYBIND_SPECIFIC_KEYS;
        private boolean jesusEnabled = Defaults.JESUS_ENABLED;
        private boolean criticalsEnabled = Defaults.CRITICALS_ENABLED;
        private double criticalsSpoofHeight = Defaults.CRITICALS_SPOOF_HEIGHT;
        private boolean durabilitySwapEnabled = Defaults.DURABILITY_SWAP_ENABLED;
        private boolean elytraBoostEnabled = Defaults.ELYTRA_BOOST_ENABLED;
        private boolean elytraDontConsumeFirework = Defaults.ELYTRA_DONT_CONSUME_FIREWORK;
        private int elytraFireworkLevel = Defaults.ELYTRA_FIREWORK_LEVEL;
        private boolean elytraPlaySound = Defaults.ELYTRA_PLAY_SOUND;
        private boolean elytraSwapEnabled = Defaults.ELYTRA_SWAP_ENABLED;
        private boolean f5TweaksEnabled = Defaults.F5_TWEAKS_ENABLED;
        private boolean fastAttackEnabled = Defaults.FAST_ATTACK_ENABLED;
        private int fastAttackTimesPerTick = Defaults.FAST_ATTACK_TIMES_PER_TICK;
        private boolean attributeSwapEnabled = Defaults.ATTRIBUTE_SWAP_ENABLED;
        private boolean fastUseEnabled = Defaults.FAST_USE_ENABLED;
        private int fastUseTimesPerTick = Defaults.FAST_USE_TIMES_PER_TICK;
        private boolean guiMoveEnabled = Defaults.GUI_MOVE_ENABLED;
        private boolean holdAttackEnabled = Defaults.HOLD_ATTACK_ENABLED;
        private boolean holdUseEnabled = Defaults.HOLD_USE_ENABLED;
        private boolean itemRestockEnabled = Defaults.ITEM_RESTOCK_ENABLED;
        private boolean hotbarRowSwapEnabled = Defaults.HOTBAR_ROW_SWAP_ENABLED;
        private boolean pearlBoostEnabled = Defaults.PEARL_BOOST_ENABLED;
        private double pearlBoostVelocity = Defaults.PEARL_BOOST_VELOCITY;
        private boolean periodicAttackEnabled = Defaults.PERIODIC_ATTACK_ENABLED;
        private int periodicAttackDelayTicks = Defaults.PERIODIC_ATTACK_DELAY_TICKS;
        private boolean periodicUseEnabled = Defaults.PERIODIC_USE_ENABLED;
        private int periodicUseDelayTicks = Defaults.PERIODIC_USE_DELAY_TICKS;
        private boolean pickBeforePlaceEnabled = Defaults.PICK_BEFORE_PLACE_ENABLED;
        private boolean renderInvisibilityEnabled = Defaults.RENDER_INVISIBILITY_ENABLED;
        private boolean highJumpEnabled = Defaults.HIGH_JUMP_ENABLED;
        private double highJumpMultiplier = Defaults.HIGH_JUMP_MULTIPLIER;
        private boolean longJumpEnabled = Defaults.LONG_JUMP_ENABLED;
        private double longJumpMomentum = Defaults.LONG_JUMP_MOMENTUM;
        private boolean aerodynamicsEnabled = Defaults.AERODYNAMICS_ENABLED;
        private double aerodynamicsAcceleration = Defaults.AERODYNAMICS_ACCELERATION;
        private boolean sneakEnabled = Defaults.SNEAK_ENABLED;
        private boolean stepEnabled = Defaults.STEP_ENABLED;
        private double stepHeight = Defaults.STEP_HEIGHT;
        private boolean airJumpEnabled = Defaults.AIR_JUMP_ENABLED;
        private boolean flightEnabled = Defaults.FLIGHT_ENABLED;
        private boolean freeCamEnabled = Defaults.FREE_CAM_ENABLED;
        private String speedMineMode = Defaults.SPEED_MINE_MODE;
        private boolean BreachSwapEnabled = Defaults.BREACH_SWAP_ENABLED;
        private boolean PearlCatchEnabled = Defaults.PEARL_CATCH_ENABLED;

        private static ConfigData captureCurrentState() {
            ConfigData data = new ConfigData();
            data.sprintEnabled = Sprint.enabled;
            data.autoRespawnEnabled = AutoRespawn.enabled;
            data.antiHungerEnabled = AntiHunger.enabled;
            data.noFallEnabled = NoFall.enabled;
            data.autoToolEnabled = AutoTool.enabled;
            data.ghostHandEnabled = GhostHand.enabled;
            data.portalsEnabled = disablePortalGuiClosing.enabled;
            data.deadMobInteractionEnabled = disableDeadMobInteraction.enabled;
            data.axeStrippingEnabled = disableAxeStripping.enabled;
            data.shovelPathingEnabled = disableShovelPathing.enabled;
            data.blockBreakingCooldownEnabled = disableBlockBreakingCooldown.enabled;
            data.blockBreakingParticlesEnabled = disableBlockBreakingParticles.enabled;
            data.inventoryEffectRenderingEnabled = disableInventoryEffectRendering.enabled;
            data.nauseaOverlayEnabled = disableNauseaOverlay.enabled;
            data.netherPortalSoundEnabled = disableNetherPortalSound.enabled;
            data.fogRenderingEnabled = disableFogRendering.enabled;
            data.firstPersonEffectParticlesEnabled = disableFirstPersonEffectParticles.enabled;
            data.rainEffectsEnabled = disableRainEffects.enabled;
            data.deadMobRenderingEnabled = disableDeadMobRendering.enabled;
            data.tridentBoostEnabled = TridentBoost.enabled;
            data.blinkKeybindEnabled = Blink.CanUseKeybind;
            data.keybindModifierKey = ZephyrKeybindManager.getModifierKeyCode();
            data.keybindSpecificKeys = ZephyrKeybindManager.getSpecificKeyCodes();
            data.jesusEnabled = Jesus.enabled;
            data.criticalsEnabled = Criticals.enabled;
            data.criticalsSpoofHeight = Criticals.getSpoofHeight();
            data.durabilitySwapEnabled = DurabilitySwap.enabled;
            data.elytraBoostEnabled = ElytraBoost.enabled;
            data.elytraDontConsumeFirework = ElytraBoost.dontConsumeFirework;
            data.elytraFireworkLevel = ElytraBoost.fireworkLevel;
            data.elytraPlaySound = ElytraBoost.playSound;
            data.elytraSwapEnabled = ElytraSwap.enabled;
            data.f5TweaksEnabled = F5Tweaks.enabled;
            data.fastAttackEnabled = FastAttack.enabled;
            data.fastAttackTimesPerTick = FastAttack.getTimesPerTick();
            data.attributeSwapEnabled = ShieldBreaker.enabled;
            data.fastUseEnabled = FastUse.enabled;
            data.fastUseTimesPerTick = FastUse.getTimesPerTick();
            data.guiMoveEnabled = GuiMove.enabled;
            data.holdAttackEnabled = HoldAttack.enabled;
            data.holdUseEnabled = HoldUse.enabled;
            data.itemRestockEnabled = ItemRestock.enabled;
            data.hotbarRowSwapEnabled = HotbarRowSwap.enabled;
            data.pearlBoostEnabled = PearlBoost.enabled;
            data.pearlBoostVelocity = PearlBoost.getBoostVelocity();
            data.periodicAttackEnabled = PeriodicAttack.enabled;
            data.periodicAttackDelayTicks = PeriodicAttack.getDelayTicks();
            data.periodicUseEnabled = PeriodicUse.enabled;
            data.periodicUseDelayTicks = PeriodicUse.getDelayTicks();
            data.pickBeforePlaceEnabled = PickBeforePlace.enabled;
            data.renderInvisibilityEnabled = RenderInvisibility.enabled;
            data.highJumpEnabled = HighJump.enabled;
            data.highJumpMultiplier = HighJump.getMultiplier();
            data.longJumpEnabled = LongJump.enabled;
            data.longJumpMomentum = LongJump.getMomentum();
            data.aerodynamicsEnabled = Aerodynamics.enabled;
            data.aerodynamicsAcceleration = Aerodynamics.getAcceleration();
            data.sneakEnabled = Sneak.enabled;
            data.stepEnabled = Step.isEnabled();
            data.stepHeight = Step.getStepHeight();
            data.airJumpEnabled = AirJump.enabled;
            data.flightEnabled = Flight.enabled;
            data.freeCamEnabled = FreeCam.enabled;
            data.speedMineMode = SpeedMine.mode.name();
            data.BreachSwapEnabled = BreachSwap.enabled;
            data.PearlCatchEnabled = PearlCatch.enabled;
            return data;
        }

        private SpeedMine.Mode getSpeedMineMode() {
            try {
                return SpeedMine.Mode.valueOf(speedMineMode);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return SpeedMine.Mode.OFF;
            }
        }
    }
}
