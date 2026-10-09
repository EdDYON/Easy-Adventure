package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
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
    private int currentX;
    private int currentY;
    private int currentZ;

    public CoreSizeScreen(CoreSizeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft.level.getBlockEntity(menu.getPos()) instanceof BaseCoreBlockEntity core) {
            this.currentX = core.getSizeX();
            this.currentY = core.getSizeY();
            this.currentZ = core.getSizeZ();
        } else {
            this.currentX = 9;
            this.currentY = 5;
            this.currentZ = 9;
        }

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        this.xEdit = new EditBox(this.font, startX + 60, startY + 20, 50, 20, Component.literal("X"));
        this.xEdit.setValue(String.valueOf(currentX));
        this.xEdit.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(xEdit);

        this.yEdit = new EditBox(this.font, startX + 60, startY + 50, 50, 20, Component.literal("Y"));
        this.yEdit.setValue(String.valueOf(currentY));
        this.yEdit.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(yEdit);

        this.zEdit = new EditBox(this.font, startX + 60, startY + 80, 50, 20, Component.literal("Z"));
        this.zEdit.setValue(String.valueOf(currentZ));
        this.zEdit.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(zEdit);

        this.addRenderableWidget(Button.builder(Component.literal("应用设置"), button -> save())
                .pos(startX + 40, startY + 115).size(96, 20).build());
    }

    private void save() {
        try {
            String xVal = xEdit.getValue();
            String yVal = yEdit.getValue();
            String zVal = zEdit.getValue();

            int newX = xVal.isEmpty() ? 9 : Integer.parseInt(xVal);
            int newY = yVal.isEmpty() ? 5 : Integer.parseInt(yVal);
            int newZ = zVal.isEmpty() ? 9 : Integer.parseInt(zVal);

            newX = Math.max(3, newX);
            newZ = Math.max(3, newZ);
            newY = Math.max(2, newY);

            PacketDistributor.sendToServer(new UpdateCoreSizePayload(menu.getPos(), newX, newY, newZ));

            this.onClose();
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        guiGraphics.drawString(this.font, "长度 (X):", startX + 10, startY + 26, 0xFFFFFF);
        guiGraphics.drawString(this.font, "高度 (Y):", startX + 10, startY + 56, 0xFFFFFF);
        guiGraphics.drawString(this.font, "宽度 (Z):", startX + 10, startY + 86, 0xFFFFFF);

        guiGraphics.drawCenteredString(this.font, "§7建议使用奇数(如5,7)以使核心居中", startX + imageWidth / 2, startY + 145, 0xAAAAAA);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;
        guiGraphics.fill(startX, startY, startX + imageWidth, startY + imageHeight, 0xFF333333);
        guiGraphics.renderOutline(startX, startY, imageWidth, imageHeight, 0xFFFFFFFF);
    }
}