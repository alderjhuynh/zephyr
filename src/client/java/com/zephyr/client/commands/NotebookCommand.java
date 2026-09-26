package com.zephyr.client.commands;

import com.zephyr.client.notebook.NotebookScreen;
import com.zephyr.client.notebook.NotebookStorage;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Client-side notebook command. Opens a Book & Quill style editor that
 * persists entirely on the client via {@link NotebookStorage}.
 * <p>
 * Usage: {@code .z notebook} – open notebook,
 * {@code .z notebook clear} – clear all pages.
 */
public final class NotebookCommand extends Command {
    public static final NotebookCommand INSTANCE = new NotebookCommand();

    private NotebookCommand() {
        super("notebook", "Open personal notebook (book & quill UI, client-side persistent): .z notebook [clear]");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("clear");
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            NotebookStorage.clear();
            CommandManager.sendMessage("Notebook cleared.");
            return;
        }
        if (args.length != 0) {
            CommandManager.sendMessage("Usage: .z notebook [clear]");
            return;
        }
        List<String> pages = NotebookStorage.getPages();
        Minecraft mc = Minecraft.getInstance();
        // Ensure we run on the client thread - Gui owns setScreen in 26.2
        mc.execute(() -> mc.gui.setScreen(new NotebookScreen(pages)));
    }
}
