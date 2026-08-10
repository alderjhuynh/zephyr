package com.zephyr.client.commands;

import java.util.List;

/**
 * Abstract base class for all chat commands handled by the Zephyr client.
 * Subclasses provide a name, a human-readable description, and the logic
 * executed when the command is dispatched. Commands are registered through
 * {@link CommandManager} and resolved from chat input by the configured prefix.
 */
public abstract class Command {
    private final String name;
    private final String description;

    /**
     * Creates a command with the given name and description.
     *
     * @param name        the command name, matched case-insensitively in chat
     * @param description a short human-readable summary of what the command does
     */
    protected Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Returns the command name, as typed after the command prefix. */
    public final String getName() {
        return name;
    }

    /** Returns the human-readable description of the command. */
    public final String getDescription() {
        return description;
    }

    /**
     * Executes the command with the parsed arguments (each token split on
     * whitespace, with double-quoted spans kept together).
     *
     * @param args the positional arguments following the command name, may be empty
     */
    public abstract void execute(String[] args);

    /**
     * Provides tab-completion suggestions for the given argument list. The last
     * element of {@code args} is the partially typed token being completed.
     *
     * @param args the command arguments parsed up to the cursor
     * @return candidate completions; defaults to no suggestions
     */
    public List<String> suggest(String[] args) {
        return List.of();
    }
}
