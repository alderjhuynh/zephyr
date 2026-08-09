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

    public void setIcon(Element icon) {
        this.icon = icon;
    }

    public Element getIcon() {
        return icon;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public List<Line> lines() {
        return Collections.unmodifiableList(lines);
    }

    public static final class Line {
        private final List<Element> elements = new ArrayList<>();
        private int marginBottom = 2;

        public List<Element> elements() {
            return elements;
        }

        public int marginBottom() {
            return marginBottom;
        }

        public void setMarginBottom(int marginBottom) {
            this.marginBottom = marginBottom;
        }
    }
}
