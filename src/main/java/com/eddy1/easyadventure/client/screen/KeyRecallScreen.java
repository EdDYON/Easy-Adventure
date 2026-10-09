package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.menu.KeyRecallMenu;
import com.eddy1.easyadventure.network.EasyAdventureNetwork;
import com.eddy1.easyadventure.network.RecallBasePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class KeyRecallScreen extends AbstractContainerScreen<KeyRecallMenu> {
    private static final int ROWS_PER_PAGE = 6;
    private static final int COLOR_BG = 0xFF17131D;
    private static final int COLOR_PANEL = 0xFF261B2D;
    private static final int COLOR_BORDER = 0xFF695174;
    private static final int COLOR_ACCENT = 0xFF72D6D7;
    private static final int COLOR_TEXT = 0xFFE8E1F0;
    private static final int COLOR_MUTED = 0xFFA69AAE;

    private int page;
    private Button previousButton;
    private Button nextButton;
    private final Button[] recallButtons = new Button[ROWS_PER_PAGE];

    public KeyRecallScreen(KeyRecallMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 284;
        this.imageHeight = 184;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int left = screenLeft();
        int top = screenTop();

        for (int i = 0; i < recallButtons.length; i++) {
            final int row = i;
            recallButtons[i] = Button.builder(Component.translatable("button.easyadventure.recall"), button -> recall(row))
                    .pos(left + 204, top + 44 + i * 18)
                    .size(56, 16)
                    .build();
            addRenderableWidget(recallButtons[i]);
        }

        previousButton = Button.builder(Component.literal("<"), button -> changePage(-1))
                .pos(left + 24, top + 154)
                .size(28, 16)
                .build();
        nextButton = Button.builder(Component.literal(">"), button -> changePage(1))
                .pos(left + 232, top + 154)
                .size(28, 16)
                .build();
        addRenderableWidget(previousButton);
        addRenderableWidget(nextButton);
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = screenLeft();
        int top = screenTop();
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, COLOR_BG);
        guiGraphics.fill(left + 12, top + 32, left + imageWidth - 12, top + 144, COLOR_PANEL);
        guiGraphics.renderOutline(left, top, imageWidth, imageHeight, COLOR_BORDER);
        guiGraphics.renderOutline(left + 12, top + 32, imageWidth - 24, 112, COLOR_BORDER);
        guiGraphics.fill(left + 12, top + 32, left + imageWidth - 12, top + 34, COLOR_ACCENT);

        guiGraphics.drawString(this.font, this.title, left + 16, top + 14, COLOR_TEXT, false);
        List<KeyRecallMenu.RecallEntry> entries = menu.getEntries();
        if (entries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.recall_empty").withStyle(ChatFormatting.GRAY), left + 28, top + 76, COLOR_MUTED, false);
            return;
        }

        int start = page * ROWS_PER_PAGE;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            int index = start + row;
            if (index >= entries.size()) {
                continue;
            }
            KeyRecallMenu.RecallEntry entry = entries.get(index);
            int y = top + 48 + row * 18;
            int rowColor = row % 2 == 0 ? 0x222F2436 : 0x221A1522;
            guiGraphics.fill(left + 20, y - 4, left + 196, y + 12, rowColor);
            guiGraphics.drawString(this.font, trim(entry.baseName(), 126), left + 28, y, COLOR_TEXT, false);
            guiGraphics.drawString(this.font, Component.translatable("message.easyadventure.registry_state." + entry.state()), left + 146, y, COLOR_MUTED, false);
        }

        int maxPage = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        guiGraphics.drawCenteredString(this.font, Component.literal((page + 1) + " / " + maxPage), left + imageWidth / 2, top + 158, COLOR_MUTED);
    }

    private void recall(int row) {
        int index = page * ROWS_PER_PAGE + row;
        List<KeyRecallMenu.RecallEntry> entries = menu.getEntries();
        if (index < 0 || index >= entries.size()) {
            return;
        }
        EasyAdventureNetwork.sendToServer(new RecallBasePayload(menu.getPos(), entries.get(index).coreUuid()));
        onClose();
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (menu.getEntries().size() - 1) / ROWS_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        refreshButtons();
    }

    private void refreshButtons() {
        int start = page * ROWS_PER_PAGE;
        for (int row = 0; row < recallButtons.length; row++) {
            recallButtons[row].visible = start + row < menu.getEntries().size();
            recallButtons[row].active = recallButtons[row].visible;
        }
        previousButton.active = page > 0;
        nextButton.active = (page + 1) * ROWS_PER_PAGE < menu.getEntries().size();
    }

    private Component trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return Component.literal(text);
        }
        return Component.literal(this.font.plainSubstrByWidth(text, maxWidth - this.font.width("...")) + "...");
    }

    private int screenLeft() {
        return (this.width - this.imageWidth) / 2;
    }

    private int screenTop() {
        return (this.height - this.imageHeight) / 2;
    }
}
