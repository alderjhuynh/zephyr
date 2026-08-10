package com.zephyr.client.module.qol.shulkerboxtooltip.util;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * General helpers shared across the ShulkerBoxTooltip module.
 */
public final class ShulkerBoxTooltipUtil {
    /** Static utility; not instantiable. */
    private ShulkerBoxTooltipUtil() {
    }

    /**
     * Builds an identifier in Zephyr's namespace.
     *
     * @param id the path within the {@code zephyr} namespace
     * @return the fully qualified identifier
     */
    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath("zephyr", id);
    }

    /**
     * Abbreviates large item counts with a unit suffix (e.g. 1,000,000 -> 1M).
     *
     * @param count the item count to abbreviate
     * @return the abbreviated string
     */
    public static String abbreviateInteger(int count) {
        if (count == Integer.MIN_VALUE)
            return "-2G";
        if (count > -1000 && count < 1000)
            return Integer.toString(count);

        var str = new StringBuilder();

        if (count < 0) {
            str.append('-');
            count = -count;
        }
        char unit;
        int integral;
        int decimal = 0;

        switch ((int) Math.log10(count)) {
            case 3 -> {
                integral = count / 1_000;
                decimal = (count % 1_000) / 100;
                unit = 'k';
            }
            case 4, 5 -> {
                integral = count / 1_000;
                unit = 'k';
            }
            case 6 -> {
                integral = count / 1_000_000;
                decimal = (count % 1_000_000) / 100_000;
                unit = 'M';
            }
            case 7, 8 -> {
                integral = count / 1_000_000;
                unit = 'M';
            }
            default -> {
                integral = count / 1_000_000_000;
                decimal = (count % 1_000_000_000) / 100_000_000;
                unit = 'G';
            }
        }

        str.append(integral);
        if (decimal > 0)
            str.append('.').append(decimal);
        str.append(unit);
        return str.toString();
    }

    /**
     * Applies a consumer to pairs of elements from two lists, stopping at the
     * shorter list's length.
     *
     * @param left     the first list
     * @param right    the second list
     * @param consumer the operation applied to each zipped pair
     */
    public static <L, R> void zipApply(List<L> left, List<R> right, BiConsumer<L, R> consumer) {
        int size = Math.min(left.size(), right.size());
        for (int i = 0; i < size; ++i) {
            consumer.accept(left.get(i), right.get(i));
        }
    }
}
