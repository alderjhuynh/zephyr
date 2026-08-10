package com.zephyr.client.module.qol.seedcracker;

import com.zephyr.client.module.qol.Seedcracker;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.decorator.DesertWell;
import com.seedfinding.mcfeature.decorator.EndGateway;
import com.seedfinding.mcfeature.structure.BuriedTreasure;
import com.seedfinding.mcfeature.structure.DesertPyramid;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.Igloo;
import com.seedfinding.mcfeature.structure.JunglePyramid;
import com.seedfinding.mcfeature.structure.Monument;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.SwampHut;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.DeepDungeon;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.Dungeon;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.EmeraldOre;
import com.zephyr.client.module.qol.seedcracker.cracker.decorator.WarpedFungus;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.structures.TrialChambers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Holds every seedfinding {@link Feature} instance used by the seedcracker.
 *
 * <p>Features are constructed once per selected {@link MCVersion} in {@link #init(MCVersion)}
 * and exposed as static singletons so finders and the cracker can reference them. Structures
 * that participate in region-based seed reduction are additionally collected in
 * {@link #STRUCTURE_TYPES}.
 */
public class Features {
    /** All region-based structure features currently available for the active version. */
    public static final ArrayList<RegionStructure<?, ?>> STRUCTURE_TYPES = new ArrayList<>();

    public static BuriedTreasure BURIED_TREASURE;
    public static DesertPyramid DESERT_PYRAMID;
    public static EndCity END_CITY;
    public static JunglePyramid JUNGLE_PYRAMID;
    public static Monument MONUMENT;
    public static Shipwreck SHIPWRECK;
    public static SwampHut SWAMP_HUT;
    public static PillagerOutpost PILLAGER_OUTPOST;
    public static Igloo IGLOO;
    public static TrialChambers TRIAL_CHAMBERS;

    public static EndGateway END_GATEWAY;
    public static DesertWell DESERT_WELL;
    public static EmeraldOre EMERALD_ORE;
    public static Dungeon DUNGEON;
    public static DeepDungeon DEEP_DUNGEON;
    public static WarpedFungus WARPED_FUNGUS;

    /**
     * (Re)initialises every feature singleton for the given Minecraft version, clearing the
     * previously built {@link #STRUCTURE_TYPES} list first. Features that fail to construct are
     * disabled via their associated {@link Finder.Type} toggle and skipped.
     *
     * @param version the Minecraft version to build features for
     */
    public static void init(MCVersion version) {
        STRUCTURE_TYPES.clear();

        BURIED_TREASURE = safe(STRUCTURE_TYPES, Finder.Type.BURIED_TREASURE, () -> new BuriedTreasure(version));
        DESERT_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.DESERT_TEMPLE, () -> new DesertPyramid(version));
        END_CITY = safe(STRUCTURE_TYPES, Finder.Type.END_CITY, () -> new EndCity(version));
        JUNGLE_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.JUNGLE_TEMPLE, () -> new JunglePyramid(version));
        MONUMENT = safe(STRUCTURE_TYPES, Finder.Type.MONUMENT, () -> new Monument(version));
        SHIPWRECK = safe(STRUCTURE_TYPES, Finder.Type.SHIPWRECK, () -> new Shipwreck(version));
        SWAMP_HUT = safe(STRUCTURE_TYPES, Finder.Type.SWAMP_HUT, () -> new SwampHut(version));
        PILLAGER_OUTPOST = safe(STRUCTURE_TYPES, Finder.Type.PILLAGER_OUTPOST, () -> new PillagerOutpost(version));
        IGLOO = safe(STRUCTURE_TYPES, Finder.Type.IGLOO, () -> new Igloo(version));
        TRIAL_CHAMBERS = safe(STRUCTURE_TYPES, Finder.Type.TRIAL_CHAMBERS, () -> new TrialChambers(version));

        END_GATEWAY = safe(Finder.Type.END_GATEWAY, () -> new EndGateway(version));
        DESERT_WELL = safe(Finder.Type.DESERT_WELL, () -> new DesertWell(version));
        EMERALD_ORE = safe(Finder.Type.EMERALD_ORE, () -> new EmeraldOre(version));
        DUNGEON = safe(Finder.Type.DUNGEON, () -> new Dungeon(version));
        DEEP_DUNGEON = safe(Finder.Type.DUNGEON, () -> new DeepDungeon(version));
        WARPED_FUNGUS = safe(Finder.Type.WARPED_FUNGUS, () -> new WarpedFungus(version));

        STRUCTURE_TYPES.trimToSize();
    }

    /**
     * Constructs a feature through the given supplier, disabling the finder type and returning
     * null if construction fails.
     *
     * @param finderType the finder type to disable on failure
     * @param lambda the feature constructor
     * @return the constructed feature, or null on failure
     */
    private static <F extends Feature<?, ?>> F safe(Finder.Type finderType, Supplier<F> lambda) {
        try {
            return lambda.get();
        } catch (Throwable t) {
            Seedcracker.LOGGER.error("Exception thrown loading feature", t);
            finderType.enabled.set(false);
            return null;
        }
    }

    /**
     * Constructs a region structure feature and, on success, appends it to the given list.
     *
     * @param list the list to add successfully constructed structures to
     * @param finderType the finder type to disable on failure
     * @param lambda the feature constructor
     * @return the constructed feature, or null on failure
     */
    private static <F extends RegionStructure<?, ?>> F safe(List<RegionStructure<?, ?>> list, Finder.Type finderType, Supplier<F> lambda) {
        F initializedFeature = safe(finderType, lambda);
        if (initializedFeature != null) list.add(initializedFeature);
        return initializedFeature;
    }

}
