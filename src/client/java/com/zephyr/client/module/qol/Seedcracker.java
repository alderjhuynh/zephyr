package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.seedcracker.api.SeedCrackerAPI;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.finder.FinderQueue;
import com.zephyr.client.module.qol.seedcracker.util.FeatureToggle;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class Seedcracker extends Module {
    public static final Seedcracker INSTANCE = new Seedcracker();
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Seedcracker");

    public static final List<SeedCrackerAPI> entrypoints = new ArrayList<>();

    private final DataStorage dataStorage = new DataStorage();

    private final BooleanSetting active = new BooleanSetting("Active", true);
    private final BooleanSetting buriedTreasure = new BooleanSetting("Buried Treasure", true);
    private final BooleanSetting desertTemple = new BooleanSetting("Desert Temple", true);
    private final BooleanSetting endCity = new BooleanSetting("End City", true);
    private final BooleanSetting jungleTemple = new BooleanSetting("Jungle Temple", true);
    private final BooleanSetting monument = new BooleanSetting("Monument", true);
    private final BooleanSetting swampHut = new BooleanSetting("Swamp Hut", true);
    private final BooleanSetting shipwreck = new BooleanSetting("Shipwreck", true);
    private final BooleanSetting outpost = new BooleanSetting("Pillager Outpost", true);
    private final BooleanSetting igloo = new BooleanSetting("Igloo", true);
    private final BooleanSetting trialChambers = new BooleanSetting("Trial Chambers", true);
    private final BooleanSetting endPillars = new BooleanSetting("End Pillars", true);
    private final BooleanSetting endGateway = new BooleanSetting("End Gateway", false);
    private final BooleanSetting dungeon = new BooleanSetting("Dungeon", true);
    private final BooleanSetting emeraldOre = new BooleanSetting("Emerald Ore", false);
    private final BooleanSetting desertWell = new BooleanSetting("Desert Well", false);
    private final BooleanSetting warpedFungus = new BooleanSetting("Warped Fungus", false);
    private final BooleanSetting biome = new BooleanSetting("Biome", false);
    private final EnumSetting<Config.RenderType> render =
            new EnumSetting<>("Render Mode", Config.RenderType.XRAY);
    private final BooleanSetting debug = new BooleanSetting("Debug", false);
    private final BooleanSetting antiXrayBypass = new BooleanSetting("Anti-Xray Bypass", true);

    private Seedcracker() {
        super("Seedcracker", "Cracks the world seed by scanning generated structures", Category.QOL);

        addSetting(active);
        addSetting(buriedTreasure);
        addSetting(desertTemple);
        addSetting(endCity);
        addSetting(jungleTemple);
        addSetting(monument);
        addSetting(swampHut);
        addSetting(shipwreck);
        addSetting(outpost);
        addSetting(igloo);
        addSetting(trialChambers);
        addSetting(endPillars);
        addSetting(endGateway);
        addSetting(dungeon);
        addSetting(emeraldOre);
        addSetting(desertWell);
        addSetting(warpedFungus);
        addSetting(biome);
        addSetting(render);
        addSetting(debug);
        addSetting(antiXrayBypass);

        Features.init(Config.get().getVersion());
    }

    public static Seedcracker get() {
        return INSTANCE;
    }

    public DataStorage getDataStorage() {
        return this.dataStorage;
    }

    /** Wipes all collected data and running finders (called when leaving the server). */
    public void reset() {
        this.dataStorage.clear();
        FinderQueue.get().clear();
    }

    private static void setToggle(FeatureToggle toggle, boolean value) {
        toggle.set(value);
    }

    private void syncConfig() {
        Config cfg = Config.get();
        cfg.active = active.get();
        cfg.debug = debug.get();
        cfg.antiXrayBypass = antiXrayBypass.get();
        cfg.render = render.get();

        setToggle(cfg.buriedTreasure, buriedTreasure.get());
        setToggle(cfg.desertTemple, desertTemple.get());
        setToggle(cfg.endCity, endCity.get());
        setToggle(cfg.jungleTemple, jungleTemple.get());
        setToggle(cfg.monument, monument.get());
        setToggle(cfg.swampHut, swampHut.get());
        setToggle(cfg.shipwreck, shipwreck.get());
        setToggle(cfg.outpost, outpost.get());
        setToggle(cfg.igloo, igloo.get());
        setToggle(cfg.trialChambers, trialChambers.get());
        setToggle(cfg.endPillars, endPillars.get());
        setToggle(cfg.endGateway, endGateway.get());
        setToggle(cfg.dungeon, dungeon.get());
        setToggle(cfg.emeraldOre, emeraldOre.get());
        setToggle(cfg.desertWell, desertWell.get());
        setToggle(cfg.warpedFungus, warpedFungus.get());
        setToggle(cfg.biome, biome.get());
    }

    public void setActive(boolean value) {
        active.set(value);
        syncConfig();
    }

    public void setDebug(boolean value) {
        debug.set(value);
        syncConfig();
    }

    public void setRender(Config.RenderType value) {
        render.set(value);
        syncConfig();
    }

    public void setFinderEnabled(Finder.Type type, boolean value) {
        settingFor(type).set(value);
        syncConfig();
    }

    private BooleanSetting settingFor(Finder.Type type) {
        return switch (type) {
            case BURIED_TREASURE -> buriedTreasure;
            case DESERT_TEMPLE -> desertTemple;
            case END_CITY -> endCity;
            case JUNGLE_TEMPLE -> jungleTemple;
            case MONUMENT -> monument;
            case SWAMP_HUT -> swampHut;
            case SHIPWRECK -> shipwreck;
            case PILLAGER_OUTPOST -> outpost;
            case IGLOO -> igloo;
            case TRIAL_CHAMBERS -> trialChambers;
            case END_PILLARS -> endPillars;
            case END_GATEWAY -> endGateway;
            case DUNGEON -> dungeon;
            case EMERALD_ORE -> emeraldOre;
            case DESERT_WELL -> desertWell;
            case WARPED_FUNGUS -> warpedFungus;
            case BIOME -> biome;
        };
    }

    @Override
    protected void onEnable() {
        syncConfig();
    }

    @Override
    protected void onDisable() {
        syncConfig();
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        syncConfig();
        if (!Config.get().active) {
            return;
        }

        if (client.level == null || client.player == null) {
            return;
        }

        try (var ignored = client.collectPerTickGizmos()) {
            FinderQueue.get().renderCuboids();
        }
    }
}
