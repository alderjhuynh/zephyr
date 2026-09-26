package com.zephyr.client.notebook;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Client-side notebook screen that copies the vanilla {@code BookEditScreen} UI
 * 1:1 (background texture, page navigation, MultiLineEditBox, page indicator).
 * Text is stored locally via {@link NotebookStorage} and never sent to the server.
 */
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
        return Component.translatable("book.pageIndicator", currentPage + 1, getNumPages())
                .withColor(-16777216)
                .withoutShadow();
    }

    @Override
    protected void init() {
        int bgLeft = backgroundLeft();
        int bgTop = backgroundTop();

        this.page = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setTextColor(-16777216)
                .setCursorColor(-16777216)
                .setShowBackground(false)
                .setTextShadow(false)
                .setX(this.width / 2 - 114 / 2 - 8)
                .setY(28)
                .build(this.font, 122, 134, CommonComponents.EMPTY);
        this.page.setCharacterLimit(1024);
        this.page.setLineLimit(126 / Objects.requireNonNull(this.font).lineHeight);
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
            if (this.minecraft != null && this.minecraft.gui != null) {
                this.minecraft.gui.setScreen(null);
            }
        }).pos(this.width / 2 - 98 - 2, menuControlsTop()).width(98).build());

        // Done button – saves and closes
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            saveChanges();
            if (this.minecraft != null && this.minecraft.gui != null) {
                this.minecraft.gui.setScreen(null);
            }
        }).pos(this.width / 2 + 2, menuControlsTop()).width(98).build());

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
        this.page.setValue(pages.get(currentPage), true);
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
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        // Page turn with PageUp/PageDown like vanilla
        if (keyEvent.key() == 266) { // Page Up
            this.backButton.onPress(keyEvent);
            return true;
        }
        if (keyEvent.key() == 267) { // Page Down
            this.forwardButton.onPress(keyEvent);
            return true;
        }
        return super.keyPressed(keyEvent);
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
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        visitText(extractor.textRenderer());
    }

    private void visitText(ActiveTextCollector collector) {
        int bgLeft = backgroundLeft();
        int bgTop = backgroundTop();
        collector.accept(net.minecraft.client.gui.TextAlignment.RIGHT, bgLeft + 148, bgTop + 16, numberOfPages);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        extractor.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                BookViewScreen.BOOK_LOCATION,
                backgroundLeft(), backgroundTop(),
                0, 0,
                192, 192,
                256, 256
        );
    }
}
