package com.zephyr.client.commands;

import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;

    protected Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public abstract void execute(String[] args);

    public List<String> suggest(String[] args) {
        return List.of();
    }
}
