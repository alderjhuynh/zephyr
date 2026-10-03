package com.zephyr.client.module.qol.jade.tooltip;

import com.zephyr.client.module.qol.jade.render.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ordered rows of elements that make up the overlay content, mirroring Jade's
 * {@code ITooltip}: {@link #add(Element)} starts a new row, {@link #append(Element)}
 * tacks an element onto the most recent row (e.g. a bar followed by its label).
 * An optional {@link Element} icon is rendered to the left of the first row.
 */
public final class Tooltip {
    private final List<Line> lines = new ArrayList<>();
    private Element icon;

    /** Starts a new row with the given element. */
    public void add(Element element) {
        lines.add(new Line());
        lines.get(lines.size() - 1).elements.add(element);
    }

    /** Appends an element to the most recent row, or starts a new row if empty. */
    public void append(Element element) {
        if (lines.isEmpty()) {
            add(element);
        } else {
            lines.get(lines.size() - 1).elements.add(element);
        }
    }

    /** Sets the icon rendered to the left of the tooltip content. */
    public void setIcon(Element icon) {
        this.icon = icon;
    }

    /** @return the tooltip icon, or {@code null} if none was set */
    public Element getIcon() {
        return icon;
    }

    /** @return whether no content rows have been added yet */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** @return an unmodifiable view of the tooltip's rows in order */
    public List<Line> lines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * A single horizontal row of elements laid out left-to-right within the box.
     */
    public static final class Line {
        private final List<Element> elements = new ArrayList<>();
        private int marginBottom = 2;

        /** @return the elements that make up this row, in order */
        public List<Element> elements() {
            return elements;
        }

        /** @return the vertical gap (in GUI pixels) left below this row */
        public int marginBottom() {
            return marginBottom;
        }

        /** Sets the vertical gap left below this row. */
        public void setMarginBottom(int marginBottom) {
            this.marginBottom = marginBottom;
        }
    }
}
