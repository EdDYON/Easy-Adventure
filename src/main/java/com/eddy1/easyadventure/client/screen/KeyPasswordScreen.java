package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.menu.KeyPasswordMenu;
import com.eddy1.easyadventure.network.SubmitKeyPasswordPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class KeyPasswordScreen extends AbstractContainerScreen<KeyPasswordMenu> {
    private EditBox passwordEdit;

    public KeyPasswordScreen(KeyPasswordMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 196;
        this.imageHeight = 118;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        passwordEdit = new EditBox(this.font, left + 16, top + 36, 164, 20, Component.empty());
        passwordEdit.setMaxLength(CorePasswordUtil.MAX_PASSWORD_LENGTH);
        passwordEdit.setHint(Component.translatable("gui.easyadventure.password_hint"));
        this.addRenderableWidget(passwordEdit);
        setInitialFocus(passwordEdit);

        this.addRenderableWidget(Button.builder(Component.translatable("button.easyadventure.confirm"), button -> submit())
                .pos(left + 16, top + 72)
                .size(78, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.easyadventure.cancel"), button -> onClose())
                .pos(left + 102, top + 72)
                .size(78, 20)
                .build());
    }

    private void submit() {
        PacketDistributor.sendToServer(new SubmitKeyPasswordPayload(
                menu.getAction(),
                menu.getPos(),
                menu.getFace(),
                menu.getHand(),
                passwordEdit.getValue()
        ));
        onClose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.easyadventure.password"), 16, 24, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF242833);
        guiGraphics.renderOutline(left, top, imageWidth, imageHeight, 0xFFE6D18A);
    }
}
