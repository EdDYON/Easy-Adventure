package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import com.eddy1.easyadventure.network.UpdateCoreSizePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class CoreSizeScreen extends AbstractContainerScreen<CoreSizeMenu> {
    private EditBox xEdit;
    private EditBox yEdit;
    private EditBox zEdit;
    private EditBox passwordEdit;
    private Button passwordToggleButton;
    private boolean passwordEnabled;

    public CoreSizeScreen(CoreSizeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 212;
        this.imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();

        int sizeX = 9;
        int sizeY = 5;
        int sizeZ = 9;
        if (minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.getPos()) instanceof BaseCoreBlockEntity core) {
            sizeX = core.getSizeX();
            sizeY = core.getSizeY();
            sizeZ = core.getSizeZ();
        }
        passwordEnabled = menu.isPasswordEnabled();

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        this.xEdit = createNumberBox(left + 132, top + 20, sizeX, !menu.isSizeLocked());
        this.yEdit = createNumberBox(left + 132, top + 50, sizeY, !menu.isSizeLocked());
        this.zEdit = createNumberBox(left + 132, top + 80, sizeZ, !menu.isSizeLocked());
        this.passwordEdit = new EditBox(this.font, left + 16, top + 150, 180, 20, Component.empty());
        this.passwordEdit.setMaxLength(CorePasswordUtil.MAX_PASSWORD_LENGTH);
        this.passwordEdit.setHint(Component.translatable("gui.easyadventure.password_hint"));
        this.passwordEdit.setFilter(input -> input.indexOf('\n') < 0 && input.indexOf('\r') < 0 && input.length() <= CorePasswordUtil.MAX_PASSWORD_LENGTH);
        this.passwordEdit.setEditable(true);

        this.passwordToggleButton = Button.builder(Component.empty(), button -> togglePassword())
                .pos(left + 16, top + 118)
                .size(180, 20)
                .build();
        refreshToggleLabel();

        this.addRenderableWidget(xEdit);
        this.addRenderableWidget(yEdit);
        this.addRenderableWidget(zEdit);
        this.addRenderableWidget(passwordToggleButton);
        this.addRenderableWidget(passwordEdit);
        this.addRenderableWidget(Button.builder(Component.translatable("button.easyadventure.apply"), button -> save())
                .pos(left + 58, top + 186)
                .size(96, 20)
                .build());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFFFFFF);
    }

    private EditBox createNumberBox(int x, int y, int value, boolean editable) {
        EditBox box = new EditBox(this.font, x, y, 52, 20, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(input -> input.matches("\\d*"));
        box.setEditable(editable);
        box.setTextColor(editable ? 0xFFFFFF : 0x8E8E8E);
        return box;
    }

    private void togglePassword() {
        passwordEnabled = !passwordEnabled;
        refreshToggleLabel();
    }

    private void refreshToggleLabel() {
        passwordToggleButton.setMessage(Component.translatable(
                passwordEnabled ? "button.easyadventure.password_on" : "button.easyadventure.password_off"
        ));
    }

    private void save() {
        int newX = clamp(parseValue(xEdit, 9), BaseCoreBlockEntity.MIN_SIZE_XZ, BaseCoreBlockEntity.MAX_SIZE_XZ);
        int newY = clamp(parseValue(yEdit, 5), BaseCoreBlockEntity.MIN_SIZE_Y, BaseCoreBlockEntity.MAX_SIZE_Y);
        int newZ = clamp(parseValue(zEdit, 9), BaseCoreBlockEntity.MIN_SIZE_XZ, BaseCoreBlockEntity.MAX_SIZE_XZ);
        PacketDistributor.sendToServer(new UpdateCoreSizePayload(menu.getPos(), newX, newY, newZ, passwordEnabled, passwordEdit.getValue()));
        onClose();
    }

    private static int parseValue(EditBox box, int fallback) {
        String value = box.getValue();
        if (value.isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.size_x"), left + 16, top + 26, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.size_y"), left + 16, top + 56, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.size_z"), left + 16, top + 86, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.password_section"), left + 16, top + 124, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.password"), left + 16, top + 140, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.easyadventure.odd_hint"), left + imageWidth / 2, top + 214, 0xAAAAAA);
        if (menu.isSizeLocked()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.easyadventure.size_locked"), left + imageWidth / 2, top + 104, 0xF2D479);
        }
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.easyadventure.password_help"), left + imageWidth / 2, top + 174, 0xAAAAAA);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2B2F3A);
        guiGraphics.renderOutline(left, top, imageWidth, imageHeight, 0xFFE6D18A);
    }
}
