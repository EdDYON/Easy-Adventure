package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.menu.BaseNameMenu;
import com.eddy1.easyadventure.network.EasyAdventureNetwork;
import com.eddy1.easyadventure.network.SubmitBaseNamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BaseNameScreen extends AbstractContainerScreen<BaseNameMenu> {
    private static final int COLOR_BG = 0xFF17131D;
    private static final int COLOR_PANEL = 0xFF261B2D;
    private static final int COLOR_BORDER = 0xFF695174;
    private static final int COLOR_ACCENT = 0xFF72D6D7;
    private static final int COLOR_TEXT = 0xFFE8E1F0;
    private static final int COLOR_MUTED = 0xFFA69AAE;

    private EditBox nameEdit;

    public BaseNameScreen(BaseNameMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 118;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int left = screenLeft();
        int top = screenTop();
        nameEdit = new EditBox(this.font, left + 24, top + 48, 188, 20, Component.translatable("gui.easyadventure.base_name"));
        nameEdit.setMaxLength(64);
        nameEdit.setValue(menu.getCurrentName());
        nameEdit.setFilter(value -> value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.length() <= 64);
        addRenderableWidget(nameEdit);
        setInitialFocus(nameEdit);

        addRenderableWidget(Button.builder(Component.translatable("button.easyadventure.confirm"), button -> submit())
                .pos(left + 132, top + 82)
                .size(80, 20)
                .build());
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
        guiGraphics.fill(left + 12, top + 28, left + imageWidth - 12, top + 76, COLOR_PANEL);
        guiGraphics.renderOutline(left, top, imageWidth, imageHeight, COLOR_BORDER);
        guiGraphics.fill(left + 12, top + 28, left + imageWidth - 12, top + 30, COLOR_ACCENT);
        guiGraphics.drawString(this.font, this.title, left + 16, top + 12, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.base_name_hint"), left + 24, top + 34, COLOR_MUTED, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit() {
        EasyAdventureNetwork.sendToServer(new SubmitBaseNamePayload(menu.getPos(), nameEdit.getValue()));
        onClose();
    }

    private int screenLeft() {
        return (this.width - this.imageWidth) / 2;
    }

    private int screenTop() {
        return (this.height - this.imageHeight) / 2;
    }
}
