package com.zephyr.client.commands;

import com.zephyr.client.notebook.NotebookScreen;
import com.zephyr.client.notebook.NotebookStorage;
import net.minecraft.client.Minecraft;

import java.util.List;

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
        // 1.21.1: setScreen lives on Minecraft, not on Gui.
        mc.execute(() -> mc.setScreen(new NotebookScreen(pages)));
    }
}
