package com.zephyr.client.module.qol.seedcracker.cracker.storage;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A set that defers insertion until {@link #dump()} is called.
 *
 * <p>Additions are buffered in a scheduled set and only merged into the base (backing) set during
 * the time machine tick, so that iteration over the base set stays consistent while new data is
 * being collected on the main thread.
 */
public class ScheduledSet<T> implements Iterable<T> {

    /** The backing set that iteration and size operate on. */
    protected final Set<T> baseSet;
    /** Pending additions waiting to be merged on {@link #dump()}. */
    protected final Set<T> scheduledSet;

    public ScheduledSet(Comparator<T> comparator) {
        if (comparator != null) {
            this.baseSet = new TreeSet<>(comparator);
        } else {
            this.baseSet = new HashSet<>();
        }

        this.scheduledSet = new HashSet<>();
    }

    /**
     * Schedules an element for later insertion.
     *
     * @param e the element to add
     */
    public synchronized void scheduleAdd(T e) {
        this.scheduledSet.add(e);
    }

    /**
     * Merges all scheduled additions into the base set and clears the pending queue.
     */
    public synchronized void dump() {
        synchronized (this.baseSet) {
            this.baseSet.addAll(this.scheduledSet);
            this.scheduledSet.clear();
        }
    }

    /**
     * @param e the element to look up
     * @return true if the element is in the base set or is currently scheduled
     */
    public synchronized boolean contains(T e) {
        return this.baseSet.contains(e) || this.scheduledSet.contains(e);
    }

    /**
     * @return the base (backing) set
     */
    public Set<T> getBaseSet() {
        return this.baseSet;
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return this.baseSet.iterator();
    }

    /**
     * @return the size of the base set
     */
    public synchronized int size() {
        return this.baseSet.size();
    }

}
