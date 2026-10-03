package com.zephyr.client.notebook;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// Backport of 26.3's NotebookScreen. 1.21.1 differences: classic
// render()/renderBackground() instead of extractRenderState/extractBackground,
// MultiLineEditBox has a plain constructor (no builder), Button uses
// .bounds(), keyPressed takes (int, int, int), and setScreen lives on
// Minecraft, not on Gui.
public class NotebookScreen extends Screen {
    private static final Component TITLE = Component.translatable("book.edit.title");
    private static final Component SIGN_LABEL = Component.translatable("book.signButton");

    private final List<String> pages;
    private int currentPage;
    private MultiLineEditBox page;
    private PageButton forwardButton;
    private PageButton backButton;
    private Component numberOfPages = CommonComponents.EMPTY;

    public NotebookScreen(List<String> initialPages) {
        super(TITLE);
        this.pages = new ArrayList<>(initialPages);
        if (this.pages.isEmpty()) {
            this.pages.add("");
        }
    }

    private int getNumPages() {
        return pages.size();
    }

    private int backgroundLeft() {
        return (this.width - 192) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    private int menuControlsTop() {
        return backgroundTop() + 192 + 2;
    }

    private Component getPageNumberMessage() {
        return Component.translatable("book.pageIndicator", currentPage + 1, getNumPages());
    }

    @Override
    protected void init() {
        int bgLeft = backgroundLeft();
        int bgTop = backgroundTop();

        this.page = new MultiLineEditBox(this.font, this.width / 2 - 114 / 2 - 8, 28, 122, 134,
                CommonComponents.EMPTY, CommonComponents.EMPTY);
        this.page.setCharacterLimit(1024);
        this.page.setValueListener(value -> pages.set(currentPage, value));
        this.addRenderableWidget(this.page);
        updatePageContent();

        this.numberOfPages = getPageNumberMessage();

        this.backButton = (PageButton) this.addRenderableWidget(
                new PageButton(bgLeft + 43, bgTop + 157, false, button -> pageBack(), true));
        this.forwardButton = (PageButton) this.addRenderableWidget(
                new PageButton(bgLeft + 116, bgTop + 157, true, button -> pageForward(), true));

        // Sign button – for notebook it just saves and closes (no title screen)
        this.addRenderableWidget(Button.builder(SIGN_LABEL, button -> {
            saveChanges();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }).bounds(this.width / 2 - 98 - 2, menuControlsTop(), 98, 20).build());

        // Done button – saves and closes
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            saveChanges();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }).bounds(this.width / 2 + 2, menuControlsTop(), 98, 20).build());

        updateButtonVisibility();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.page);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), getPageNumberMessage());
    }

    private void pageBack() {
        if (currentPage > 0) {
            currentPage--;
            updatePageContent();
        }
        updateButtonVisibility();
    }

    private void pageForward() {
        if (currentPage < getNumPages() - 1) {
            currentPage++;
        } else {
            appendPageToBook();
            if (currentPage < getNumPages() - 1) {
                currentPage++;
            }
        }
        updatePageContent();
        updateButtonVisibility();
    }

    private void updatePageContent() {
        this.page.setValue(pages.get(currentPage));
        this.numberOfPages = getPageNumberMessage();
    }

    private void updateButtonVisibility() {
        this.backButton.visible = currentPage > 0;
    }

    private void eraseEmptyTrailingPages() {
        var it = pages.listIterator(pages.size());
        while (it.hasPrevious()) {
            if (it.previous().isEmpty()) {
                it.remove();
            } else {
                break;
            }
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
    }

    private void saveChanges() {
        // Ensure current edit box value is flushed (listener already does, but be safe)
        if (this.page != null && currentPage >= 0 && currentPage < pages.size()) {
            pages.set(currentPage, this.page.getValue());
        }
        eraseEmptyTrailingPages();
        NotebookStorage.savePages(pages);
    }

    private void appendPageToBook() {
        if (getNumPages() >= 100) {
            return;
        }
        pages.add("");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Page turn with PageUp/PageDown like vanilla (GLFW 266/267)
        if (keyCode == 266) {
            pageBack();
            return true;
        }
        if (keyCode == 267) {
            pageForward();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveChanges();
        super.onClose();
    }

    @Override
    public void removed() {
        // Ensure persistence even if closed via other means
        saveChanges();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int bgLeft = backgroundLeft();
        int bgTop = backgroundTop();
        int pageWidth = this.font.width(numberOfPages);
        graphics.drawString(this.font, numberOfPages, bgLeft - pageWidth + 192 - 44, bgTop + 16, 0xFF000000, false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.blit(BookViewScreen.BOOK_LOCATION, backgroundLeft(), backgroundTop(), 0, 0, 192, 192);
    }
}
