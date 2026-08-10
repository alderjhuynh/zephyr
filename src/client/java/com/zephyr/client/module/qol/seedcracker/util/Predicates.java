package com.zephyr.client.module.qol.seedcracker.util;

import java.util.function.BiPredicate;

/**
 * Common integer comparison predicates used by the cracker's seed filtering logic.
 */
public class Predicates {

    /** Equality: first argument equals the second. */
    public static BiPredicate<Integer, Integer> EQUAL_TO = Integer::equals;
    /** Inequality: first argument differs from the second. */
    public static BiPredicate<Integer, Integer> NOT_EQUAL_TO = (a, b) -> !a.equals(b);
    /** Strictly less than. */
    public static BiPredicate<Integer, Integer> LESS_THAN = (a, b) -> a < b;
    /** Strictly more than. */
    public static BiPredicate<Integer, Integer> MORE_THAN = (a, b) -> a > b;
    /** Less than or equal to. */
    public static BiPredicate<Integer, Integer> LESS_OR_EQUAL_TO = (a, b) -> a <= b;
    /** More than or equal to. */
    public static BiPredicate<Integer, Integer> MORE_OR_EQUAL_TO = (a, b) -> a >= b;

}
