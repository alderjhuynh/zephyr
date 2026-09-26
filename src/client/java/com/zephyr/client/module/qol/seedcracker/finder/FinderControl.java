package com.zephyr.client.module.qol.seedcracker.finder;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Tracks the finders that are currently alive per finder type.
 *
 * <p>Finders that found nothing are pruned from the map, and the remaining finders are exposed for
 * rendering.
 */
public class FinderControl {

    private final Map<Finder.Type, ConcurrentLinkedQueue<Finder>> activeFinders = new ConcurrentHashMap<>();

    /**
     * Drops every tracked finder.
     */
    public void deleteFinders() {
        this.activeFinders.clear();
    }

    /**
     * @return all active finders, after pruning the ones that found nothing
     */
    public List<Finder> getActiveFinders() {
        this.activeFinders.values().forEach(finders -> {
            finders.removeIf(Finder::isUseless);
        });

        return this.activeFinders.values().stream()
                .flatMap(Queue::stream).collect(Collectors.toList());
    }

    /**
     * Registers a finder under its type, ignoring it if it found nothing.
     *
     * @param type the type the finder belongs to
     * @param finder the finder to register
     */
    public void addFinder(Finder.Type type, Finder finder) {
        if (finder.isUseless()) return;

        if (!this.activeFinders.containsKey(type)) {
            this.activeFinders.put(type, new ConcurrentLinkedQueue<>());
        }

        this.activeFinders.get(type).add(finder);
    }
}
