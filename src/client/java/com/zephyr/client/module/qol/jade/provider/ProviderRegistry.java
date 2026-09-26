package com.zephyr.client.module.qol.jade.provider;

import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.provider.vanilla.BeehiveProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.BlockStatesProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.CropProgressProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.EntityHealthProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.HorseStatsProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.IconProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.ItemFrameProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.ModNameProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.NameProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.RedstoneProvider;
import com.zephyr.client.module.qol.jade.provider.vanilla.TntProvider;
import com.zephyr.client.module.qol.jade.render.Element;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of overlay providers, mirroring Jade's plugin/registration system. The
 * ordered lists below are the analogue of {@code VanillaPlugin}'s
 * {@code registerBlockComponent} / {@code registerEntityComponent} calls.
 */
public final class ProviderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("zephyr-jade");

    private static final List<IComponentProvider> BLOCK_PROVIDERS = new ArrayList<>();
    private static final List<IComponentProvider> ENTITY_PROVIDERS = new ArrayList<>();
    private static boolean initialized;

    /** Static utility; not instantiable. */
    private ProviderRegistry() {
    }

    /** Registers the built-in vanilla providers once; subsequent calls are no-ops. */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BLOCK_PROVIDERS.add(NameProvider.INSTANCE);
        BLOCK_PROVIDERS.add(ModNameProvider.INSTANCE);
        BLOCK_PROVIDERS.add(BlockStatesProvider.INSTANCE);
        BLOCK_PROVIDERS.add(CropProgressProvider.INSTANCE);
        BLOCK_PROVIDERS.add(RedstoneProvider.INSTANCE);
        BLOCK_PROVIDERS.add(TntProvider.INSTANCE);
        BLOCK_PROVIDERS.add(BeehiveProvider.INSTANCE);
        BLOCK_PROVIDERS.add(IconProvider.INSTANCE);

        ENTITY_PROVIDERS.add(NameProvider.INSTANCE);
        ENTITY_PROVIDERS.add(ModNameProvider.INSTANCE);
        ENTITY_PROVIDERS.add(EntityHealthProvider.INSTANCE);
        ENTITY_PROVIDERS.add(HorseStatsProvider.INSTANCE);
        ENTITY_PROVIDERS.add(ItemFrameProvider.INSTANCE);
        ENTITY_PROVIDERS.add(IconProvider.INSTANCE);
    }

    /** Runs every matching provider against the target, appending its rows. */
    public static void gather(Tooltip tooltip, Accessor accessor) {
        for (IComponentProvider provider : providers(accessor)) {
            try {
                provider.appendTooltip(tooltip, accessor);
            } catch (Exception e) {
                LOGGER.error("Jade provider {} failed", provider.getClass().getSimpleName(), e);
            }
        }
    }

    /** Runs every matching provider's icon callback; the last non-null result wins. */
    @Nullable
    public static Element gatherIcon(Accessor accessor) {
        Element icon = null;
        for (IComponentProvider provider : providers(accessor)) {
            try {
                icon = provider.getIcon(accessor, icon);
            } catch (Exception e) {
                LOGGER.error("Jade icon provider {} failed", provider.getClass().getSimpleName(), e);
            }
        }
        return icon;
    }

    private static List<IComponentProvider> providers(Accessor accessor) {
        return accessor.isBlock() ? BLOCK_PROVIDERS : ENTITY_PROVIDERS;
    }
}
