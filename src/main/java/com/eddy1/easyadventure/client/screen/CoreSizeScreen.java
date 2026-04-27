package com.eddy1.easyadventure.client.screen;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreClearMode;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.block.core.CorePermission;
import com.eddy1.easyadventure.block.core.CoreResident;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import com.eddy1.easyadventure.network.EasyAdventureNetwork;
import com.eddy1.easyadventure.network.UpdateCoreResidentPayload;
import com.eddy1.easyadventure.network.UpdateCoreSizePayload;
import com.eddy1.easyadventure.network.UpdateResidentPermissionPayload;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class CoreSizeScreen extends AbstractContainerScreen<CoreSizeMenu> {
    private static final int RESIDENTS_PER_PAGE = 3;

    private static final int TAB_W = 114;
    private static final int TAB_H = 20;

    private static final int MAIN_X = 16;
    private static final int MAIN_Y = 48;
    private static final int MAIN_W = 372;
    private static final int MAIN_H = 126;

    private static final int HOTBAR_X = 118;
    private static final int HOTBAR_Y = 188;
    private static final int HOTBAR_W = 168;
    private static final int HOTBAR_H = 30;

    private static final int GENERAL_LEFT_X = 24;
    private static final int GENERAL_RIGHT_X = 198;
    private static final int GENERAL_CARD_Y = 56;
    private static final int GENERAL_LEFT_W = 160;
    private static final int GENERAL_RIGHT_W = 178;
    private static final int GENERAL_CARD_H = 110;

    private static final int RESIDENTS_LEFT_X = 24;
    private static final int RESIDENTS_RIGHT_X = 188;
    private static final int RESIDENTS_CARD_Y = 56;
    private static final int RESIDENTS_LEFT_W = 152;
    private static final int RESIDENTS_RIGHT_W = 188;
    private static final int RESIDENTS_CARD_H = 118;

    private static final int UPGRADE_LEFT_X = 24;
    private static final int UPGRADE_RIGHT_X = 204;
    private static final int UPGRADE_TOP_Y = 56;
    private static final int UPGRADE_BOTTOM_Y = 118;
    private static final int UPGRADE_W = 172;
    private static final int UPGRADE_H = 50;

    private static final int COLOR_BG = 0xFF1A1E27;
    private static final int COLOR_PANEL = 0xFF262D3A;
    private static final int COLOR_CARD = 0xFF303949;
    private static final int COLOR_BORDER = 0xFF5D6C82;
    private static final int COLOR_ACCENT = 0xFFE6D18A;
    private static final int COLOR_TEXT = 0xFFDCE7F5;
    private static final int COLOR_MUTED = 0xFFAFBBCB;
    private static final int COLOR_SOFT = 0xFF8C98A7;
    private static final int COLOR_OK = 0xFFBDE7AF;
    private static final int COLOR_WARN = 0xFFF2D479;
    private static final int COLOR_MAGIC = 0xFF59B7C8;

    private EditBox xEdit;
    private EditBox yEdit;
    private EditBox downYEdit;
    private EditBox zEdit;
    private EditBox baseNameEdit;
    private EditBox passwordEdit;
    private Button pageGeneralButton;
    private Button pageResidentsButton;
    private Button pageUpgradesButton;
    private Button passwordToggleButton;
    private Button clearModeButton;
    private Button applyButton;
    private Button residentPickerButton;
    private Button residentCandidatePreviousButton;
    private Button residentCandidateNextButton;
    private Button addResidentButton;
    private Button removeResidentButton;
    private Button previousResidentsButton;
    private Button nextResidentsButton;
    private final Button[] residentButtons = new Button[RESIDENTS_PER_PAGE];
    private final Button[] permissionButtons = new Button[CorePermission.values().length];
    private boolean passwordEnabled;
    private CoreClearMode clearMode = CoreClearMode.CLEAR;
    private Page currentPage = Page.GENERAL;
    private int residentPage;
    private @Nullable UUID selectedResidentUuid;
    private @Nullable UUID selectedCandidateUuid;

    public CoreSizeScreen(CoreSizeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 404;
        this.imageHeight = 236;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();

        BaseCoreBlockEntity core = currentCore();
        int sizeX = core != null ? core.getSizeX() : 9;
        int sizeY = core != null ? core.getSizeY() : 5;
        int sizeBelowY = core != null ? core.getSizeBelowY() : 0;
        int sizeZ = core != null ? core.getSizeZ() : 9;
        String baseName = core != null ? core.getBaseName() : BaseCoreBlockEntity.DEFAULT_BASE_NAME;
        clearMode = core != null ? core.getClearMode() : CoreClearMode.CLEAR;
        passwordEnabled = menu.isPasswordEnabled();

        int left = screenLeft();
        int top = screenTop();

        pageGeneralButton = Button.builder(Component.translatable("button.easyadventure.page_general"), button -> setPage(Page.GENERAL))
                .pos(left + 16, top + 18)
                .size(TAB_W, TAB_H)
                .build();
        pageResidentsButton = Button.builder(Component.translatable("button.easyadventure.page_residents"), button -> setPage(Page.RESIDENTS))
                .pos(left + 145, top + 18)
                .size(TAB_W, TAB_H)
                .build();
        pageUpgradesButton = Button.builder(Component.translatable("button.easyadventure.page_upgrades"), button -> setPage(Page.UPGRADES))
                .pos(left + 274, top + 18)
                .size(TAB_W, TAB_H)
                .build();

        xEdit = createNumberBox(left + 118, top + 68, sizeX, !menu.isSizeLocked());
        yEdit = createNumberBox(left + 118, top + 92, sizeY, !menu.isSizeLocked());
        downYEdit = createNumberBox(left + 118, top + 116, sizeBelowY, !menu.isSizeLocked());
        zEdit = createNumberBox(left + 118, top + 140, sizeZ, !menu.isSizeLocked());

        baseNameEdit = new EditBox(this.font, left + 220, top + 72, 146, 18, Component.empty());
        baseNameEdit.setMaxLength(64);
        baseNameEdit.setValue(baseName);
        baseNameEdit.setFilter(input -> input.indexOf('\n') < 0 && input.indexOf('\r') < 0 && input.length() <= 64);

        clearModeButton = Button.builder(Component.empty(), button -> toggleClearMode())
                .pos(left + 220, top + 96)
                .size(146, 20)
                .build();
        refreshClearModeLabel();

        passwordToggleButton = Button.builder(Component.empty(), button -> togglePassword())
                .pos(left + 220, top + 120)
                .size(146, 20)
                .build();
        refreshToggleLabel();

        passwordEdit = new EditBox(this.font, left + 220, top + 144, 74, 18, Component.empty());
        passwordEdit.setMaxLength(CorePasswordUtil.MAX_PASSWORD_LENGTH);
        passwordEdit.setHint(Component.empty());
        passwordEdit.setFilter(input -> input.indexOf('\n') < 0 && input.indexOf('\r') < 0 && input.length() <= CorePasswordUtil.MAX_PASSWORD_LENGTH);

        applyButton = Button.builder(Component.translatable("button.easyadventure.apply"), button -> save())
                .pos(left + 300, top + 143)
                .size(66, 20)
                .build();

        residentPickerButton = Button.builder(Component.empty(), button -> {
        })
                .pos(left + 34, top + 72)
                .size(132, 18)
                .build();
        residentPickerButton.active = false;

        addResidentButton = Button.builder(Component.translatable("button.easyadventure.resident_add"), button -> addResident())
                .pos(left + 34, top + 96)
                .size(62, 18)
                .build();
        removeResidentButton = Button.builder(Component.translatable("button.easyadventure.resident_remove"), button -> removeResident())
                .pos(left + 104, top + 96)
                .size(62, 18)
                .build();

        previousResidentsButton = Button.builder(Component.literal("<"), button -> changeResidentPage(-1))
                .pos(left + 128, top + 146)
                .size(18, 14)
                .build();
        nextResidentsButton = Button.builder(Component.literal(">"), button -> changeResidentPage(1))
                .pos(left + 148, top + 146)
                .size(18, 14)
                .build();
        residentCandidatePreviousButton = Button.builder(Component.literal("<"), button -> cycleResidentCandidate(-1))
                .pos(left + 128, top + 58)
                .size(18, 14)
                .build();
        residentCandidateNextButton = Button.builder(Component.literal(">"), button -> cycleResidentCandidate(1))
                .pos(left + 148, top + 58)
                .size(18, 14)
                .build();

        for (int i = 0; i < RESIDENTS_PER_PAGE; i++) {
            final int index = i;
            residentButtons[i] = Button.builder(Component.empty(), button -> selectResident(index))
                    .pos(left + 34, top + 122 + i * 16)
                    .size(132, 14)
                    .build();
        }

        for (int i = 0; i < permissionButtons.length; i++) {
            CorePermission permission = CorePermission.values()[i];
            permissionButtons[i] = Button.builder(Component.empty(), button -> togglePermission(permission))
                    .pos(left + 316, top + 86 + i * 16)
                    .size(58, 16)
                    .build();
        }

        addRenderableWidget(pageGeneralButton);
        addRenderableWidget(pageResidentsButton);
        addRenderableWidget(pageUpgradesButton);
        addRenderableWidget(xEdit);
        addRenderableWidget(yEdit);
        addRenderableWidget(downYEdit);
        addRenderableWidget(zEdit);
        addRenderableWidget(baseNameEdit);
        addRenderableWidget(clearModeButton);
        addRenderableWidget(passwordToggleButton);
        addRenderableWidget(passwordEdit);
        addRenderableWidget(applyButton);
        addRenderableWidget(residentPickerButton);
        addRenderableWidget(residentCandidatePreviousButton);
        addRenderableWidget(residentCandidateNextButton);
        addRenderableWidget(addResidentButton);
        addRenderableWidget(removeResidentButton);
        addRenderableWidget(previousResidentsButton);
        addRenderableWidget(nextResidentsButton);

        for (Button residentButton : residentButtons) {
            addRenderableWidget(residentButton);
        }
        for (Button permissionButton : permissionButtons) {
            addRenderableWidget(permissionButton);
        }

        updatePageState();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        refreshDynamicState();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderPage(guiGraphics);
        renderUpgradeSlotOverlays(guiGraphics);
        renderUpgradeTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isEditingText()) {
            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (isEditingText()) {
            return false;
        }
        return super.checkHotbarKeyPressed(keyCode, scanCode);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = screenLeft();
        int top = screenTop();

        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, COLOR_BG);
        drawPanel(guiGraphics, left + MAIN_X, top + MAIN_Y, MAIN_W, MAIN_H);
        drawPanel(guiGraphics, left + HOTBAR_X, top + HOTBAR_Y, HOTBAR_W, HOTBAR_H);
        guiGraphics.fill(left + MAIN_X, top + MAIN_Y, left + MAIN_X + MAIN_W, top + MAIN_Y + 2, COLOR_ACCENT);
        guiGraphics.fill(left + HOTBAR_X, top + HOTBAR_Y, left + HOTBAR_X + HOTBAR_W, top + HOTBAR_Y + 2, COLOR_ACCENT);
        drawHotbarFrames(guiGraphics);

        switch (currentPage) {
            case GENERAL -> {
                drawCard(guiGraphics, left + GENERAL_LEFT_X, top + GENERAL_CARD_Y, GENERAL_LEFT_W, GENERAL_CARD_H);
                drawCard(guiGraphics, left + GENERAL_RIGHT_X, top + GENERAL_CARD_Y, GENERAL_RIGHT_W, GENERAL_CARD_H);
            }
            case RESIDENTS -> {
                drawCard(guiGraphics, left + RESIDENTS_LEFT_X, top + RESIDENTS_CARD_Y, RESIDENTS_LEFT_W, RESIDENTS_CARD_H);
                drawCard(guiGraphics, left + RESIDENTS_RIGHT_X, top + RESIDENTS_CARD_Y, RESIDENTS_RIGHT_W, RESIDENTS_CARD_H);
            }
            case UPGRADES -> {
                drawCard(guiGraphics, left + UPGRADE_LEFT_X, top + UPGRADE_TOP_Y, UPGRADE_W, UPGRADE_H);
                drawCard(guiGraphics, left + UPGRADE_RIGHT_X, top + UPGRADE_TOP_Y, UPGRADE_W, UPGRADE_H);
                drawCard(guiGraphics, left + UPGRADE_LEFT_X, top + UPGRADE_BOTTOM_Y, UPGRADE_W, UPGRADE_H);
                drawCard(guiGraphics, left + UPGRADE_RIGHT_X, top + UPGRADE_BOTTOM_Y, UPGRADE_W, UPGRADE_H);
                drawUpgradeSlotFrames(guiGraphics);
            }
        }
    }

    private void renderPage(GuiGraphics guiGraphics) {
        switch (currentPage) {
            case GENERAL -> renderGeneralPage(guiGraphics);
            case RESIDENTS -> renderResidentsPage(guiGraphics);
            case UPGRADES -> renderUpgradesPage(guiGraphics);
        }
    }

    private void renderGeneralPage(GuiGraphics guiGraphics) {
        int left = screenLeft();
        int top = screenTop();

        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.size_x"), left + GENERAL_LEFT_X + 10, top + GENERAL_CARD_Y + 20, 56, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.size_y"), left + GENERAL_LEFT_X + 10, top + GENERAL_CARD_Y + 44, 56, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.size_y_below"), left + GENERAL_LEFT_X + 10, top + GENERAL_CARD_Y + 68, 56, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.size_z"), left + GENERAL_LEFT_X + 10, top + GENERAL_CARD_Y + 92, 56, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.base_name"), left + GENERAL_RIGHT_X + 10, top + GENERAL_CARD_Y + 10, 96, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.clear_mode"), left + GENERAL_RIGHT_X + 10, top + GENERAL_CARD_Y + 34, 96, COLOR_TEXT);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.password"), left + GENERAL_RIGHT_X + 10, top + GENERAL_CARD_Y + 58, 96, COLOR_TEXT);
    }

    private void renderResidentsPage(GuiGraphics guiGraphics) {
        int left = screenLeft();
        int top = screenTop();
        BaseCoreBlockEntity core = currentCore();
        CoreResident selectedResident = selectedResident(core);
        if (selectedResident != null) {
            drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.selected_resident", selectedResident.name()), left + RESIDENTS_RIGHT_X + 10, top + RESIDENTS_CARD_Y + 10, 120, COLOR_TEXT);
        }

        for (int i = 0; i < CorePermission.values().length; i++) {
            CorePermission permission = CorePermission.values()[i];
            drawTrimmed(guiGraphics, Component.translatable(permission.translationKey()), left + RESIDENTS_RIGHT_X + 10, top + RESIDENTS_CARD_Y + 32 + i * 16, 96, COLOR_TEXT);
        }
    }

    private void renderUpgradesPage(GuiGraphics guiGraphics) {
        renderUpgradeCard(guiGraphics, CoreUpgrade.BLAST_SHIELD, UPGRADE_LEFT_X, UPGRADE_TOP_Y);
        renderUpgradeCard(guiGraphics, CoreUpgrade.GREENHOUSE, UPGRADE_RIGHT_X, UPGRADE_TOP_Y);
        renderUpgradeCard(guiGraphics, CoreUpgrade.PURIFICATION, UPGRADE_LEFT_X, UPGRADE_BOTTOM_Y);
        renderUpgradeCard(guiGraphics, CoreUpgrade.FOLDING, UPGRADE_RIGHT_X, UPGRADE_BOTTOM_Y);
    }

    private void renderUpgradeCard(GuiGraphics guiGraphics, CoreUpgrade upgrade, int cardX, int cardY) {
        int left = screenLeft();
        int top = screenTop();
        BaseCoreBlockEntity core = currentCore();
        int remainingTicks = core == null ? 0 : core.getUpgradeFuelTicks(upgrade);
        int durationTicks = Math.max(1, upgrade.durationPerFuelTicks());
        float progress = Math.min(1.0F, remainingTicks / (float) durationTicks);

        drawTrimmed(guiGraphics, Component.translatable(upgrade.translationKey()), left + cardX + 32, top + cardY + 8, 126, 0xFFFFFF);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.upgrade_remaining_short", BaseCoreBlockEntity.formatDuration(remainingTicks)), left + cardX + 32, top + cardY + 20, 126, remainingTicks > 0 ? COLOR_OK : COLOR_SOFT);
        drawMagicProgress(guiGraphics, left + cardX + 32, top + cardY + 31, 124, 10, progress, remainingTicks > 0);
        drawTrimmed(guiGraphics, Component.translatable("gui.easyadventure.upgrade_fuel_short", upgrade.material().getDescription(), BaseCoreBlockEntity.formatDuration(upgrade.durationPerFuelTicks())), left + cardX + 32, top + cardY + 43, 126, COLOR_MUTED);
    }

    private void setPage(Page page) {
        currentPage = page;
        updatePageState();
    }

    private void updatePageState() {
        boolean managerAccess = hasManagerAccess();
        if (!managerAccess && currentPage != Page.GENERAL) {
            currentPage = Page.GENERAL;
        }

        boolean generalPage = currentPage == Page.GENERAL;
        boolean residentsPage = currentPage == Page.RESIDENTS;
        boolean upgradesPage = currentPage == Page.UPGRADES;

        setWidgetState(xEdit, generalPage, !menu.isSizeLocked());
        setWidgetState(yEdit, generalPage, !menu.isSizeLocked());
        setWidgetState(downYEdit, generalPage, !menu.isSizeLocked());
        setWidgetState(zEdit, generalPage, !menu.isSizeLocked());
        setWidgetState(baseNameEdit, generalPage, true);
        setWidgetState(clearModeButton, generalPage, true);
        setWidgetState(passwordToggleButton, generalPage, true);
        setWidgetState(passwordEdit, generalPage, true);
        setWidgetState(applyButton, generalPage, true);

        setWidgetState(residentPickerButton, residentsPage, true);
        setWidgetState(residentCandidatePreviousButton, residentsPage, hasResidentCandidates());
        setWidgetState(residentCandidateNextButton, residentsPage, hasResidentCandidates());
        setWidgetState(addResidentButton, residentsPage, selectedCandidateUuid != null);
        setWidgetState(removeResidentButton, residentsPage, selectedResidentUuid != null);
        setWidgetState(previousResidentsButton, residentsPage, residentPage > 0);
        setWidgetState(nextResidentsButton, residentsPage, hasNextResidentPage());
        for (Button residentButton : residentButtons) {
            setWidgetState(residentButton, residentsPage, true);
        }
        for (Button permissionButton : permissionButtons) {
            setWidgetState(permissionButton, residentsPage, selectedResidentUuid != null);
        }

        pageGeneralButton.visible = true;
        pageGeneralButton.active = !generalPage;
        pageResidentsButton.visible = managerAccess;
        pageResidentsButton.active = managerAccess && !residentsPage;
        pageUpgradesButton.visible = managerAccess;
        pageUpgradesButton.active = managerAccess && !upgradesPage;
        menu.setUpgradeSlotsVisible(upgradesPage);

        if (generalPage) {
            setFocused(xEdit);
        } else {
            setFocused(null);
        }
    }

    private void refreshDynamicState() {
        BaseCoreBlockEntity core = currentCore();
        List<CoreResident> residents = core == null ? List.of() : core.getResidentList();
        List<ResidentCandidate> candidates = collectResidentCandidates(core);
        int maxPage = Math.max(0, (residents.size() - 1) / RESIDENTS_PER_PAGE);
        residentPage = Math.min(residentPage, maxPage);

        if (selectedResidentUuid != null && residents.stream().noneMatch(resident -> resident.uuid().equals(selectedResidentUuid))) {
            selectedResidentUuid = null;
        }
        if (selectedResidentUuid == null && !residents.isEmpty()) {
            selectedResidentUuid = residents.get(Math.min(residentPage * RESIDENTS_PER_PAGE, residents.size() - 1)).uuid();
        }
        if (selectedCandidateUuid != null && candidates.stream().noneMatch(candidate -> candidate.uuid().equals(selectedCandidateUuid))) {
            selectedCandidateUuid = null;
        }
        if (selectedCandidateUuid == null && !candidates.isEmpty()) {
            selectedCandidateUuid = candidates.get(0).uuid();
        }

        for (int i = 0; i < residentButtons.length; i++) {
            int residentIndex = residentPage * RESIDENTS_PER_PAGE + i;
            Button button = residentButtons[i];
            boolean hasResident = residentIndex < residents.size();
            button.visible = currentPage == Page.RESIDENTS && hasResident;
            button.active = currentPage == Page.RESIDENTS && hasResident;
            if (hasResident) {
                CoreResident resident = residents.get(residentIndex);
                String prefix = resident.uuid().equals(selectedResidentUuid) ? "> " : "";
                button.setMessage(Component.literal(trimText(prefix + resident.name(), 112)));
            } else {
                button.setMessage(Component.empty());
            }
        }
        residentPickerButton.setMessage(selectedCandidateLabel());

        CoreResident selectedResident = selectedResident(core);
        boolean fullAccess = selectedResident != null && selectedResident.hasFullAccess();
        boolean editingSelf = isSelectedResidentSelf(selectedResident);
        for (int i = 0; i < permissionButtons.length; i++) {
            CorePermission permission = CorePermission.values()[i];
            boolean enabled = selectedResident != null && selectedResident.hasPermission(permission);
            boolean editable = selectedResident != null && !editingSelf && (!fullAccess || permission == CorePermission.RESIZE);
            permissionButtons[i].active = currentPage == Page.RESIDENTS && editable;
            permissionButtons[i].setMessage(Component.translatable(enabled ? "button.easyadventure.status_on" : "button.easyadventure.status_off"));
        }

        previousResidentsButton.active = currentPage == Page.RESIDENTS && residentPage > 0;
        nextResidentsButton.active = currentPage == Page.RESIDENTS && hasNextResidentPage();
        addResidentButton.active = currentPage == Page.RESIDENTS && selectedCandidateUuid != null;
        removeResidentButton.active = currentPage == Page.RESIDENTS && selectedResidentUuid != null;
        residentCandidatePreviousButton.active = currentPage == Page.RESIDENTS && candidates.size() > 1;
        residentCandidateNextButton.active = currentPage == Page.RESIDENTS && candidates.size() > 1;
    }

    private void drawHotbarFrames(GuiGraphics guiGraphics) {
        for (int i = 0; i < 9; i++) {
            Slot slot = menu.slots.get(CoreUpgrade.values().length + i);
            drawSlotFrame(guiGraphics, screenLeft() + slot.x, screenTop() + slot.y);
        }
    }

    private void drawUpgradeSlotFrames(GuiGraphics guiGraphics) {
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            Slot slot = menu.slots.get(menu.getUpgradeSlotIndex(upgrade));
            drawSlotFrame(guiGraphics, screenLeft() + slot.x, screenTop() + slot.y);
        }
    }

    private void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0F1218);
        guiGraphics.renderOutline(x - 1, y - 1, 18, 18, COLOR_BORDER);
    }

    private void renderUpgradeSlotOverlays(GuiGraphics guiGraphics) {
        if (currentPage != Page.UPGRADES) {
            return;
        }

        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            renderUpgradeSlotOverlay(guiGraphics, upgrade);
        }
    }

    private void renderUpgradeSlotOverlay(GuiGraphics guiGraphics, CoreUpgrade upgrade) {
        BaseCoreBlockEntity core = currentCore();
        if (core == null) {
            return;
        }

        Slot slot = menu.slots.get(menu.getUpgradeSlotIndex(upgrade));
        int slotX = screenLeft() + slot.x;
        int slotY = screenTop() + slot.y;
        int remainingTicks = core.getUpgradeFuelTicks(upgrade);

        if (remainingTicks > 0) {
            guiGraphics.renderOutline(slotX - 1, slotY - 1, 18, 18, COLOR_WARN);
            return;
        }

        if (slot.hasItem()) {
            guiGraphics.renderOutline(slotX - 1, slotY - 1, 18, 18, COLOR_ACCENT);
        }
    }

    private void renderUpgradeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (currentPage != Page.UPGRADES) {
            return;
        }
        if (hoveringUpgradeSlot(mouseX, mouseY) != null) {
            return;
        }

        CoreUpgrade hoveredUpgrade = hoveredUpgradeCard(mouseX, mouseY);
        if (hoveredUpgrade == null) {
            return;
        }

        guiGraphics.renderComponentTooltip(
                this.font,
                List.of(
                        Component.translatable(hoveredUpgrade.translationKey()),
                        Component.translatable(hoveredUpgrade.effectTranslationKey()),
                        Component.translatable(
                                "gui.easyadventure.upgrade_fuel_short",
                                hoveredUpgrade.material().getDescription(),
                                BaseCoreBlockEntity.formatDuration(hoveredUpgrade.durationPerFuelTicks())
                        )
                ),
                mouseX,
                mouseY
        );
    }

    private @Nullable CoreUpgrade hoveredUpgradeCard(int mouseX, int mouseY) {
        if (isHoveringUpgradeCard(UPGRADE_LEFT_X, UPGRADE_TOP_Y, mouseX, mouseY)) {
            return CoreUpgrade.BLAST_SHIELD;
        }
        if (isHoveringUpgradeCard(UPGRADE_RIGHT_X, UPGRADE_TOP_Y, mouseX, mouseY)) {
            return CoreUpgrade.GREENHOUSE;
        }
        if (isHoveringUpgradeCard(UPGRADE_LEFT_X, UPGRADE_BOTTOM_Y, mouseX, mouseY)) {
            return CoreUpgrade.PURIFICATION;
        }
        if (isHoveringUpgradeCard(UPGRADE_RIGHT_X, UPGRADE_BOTTOM_Y, mouseX, mouseY)) {
            return CoreUpgrade.FOLDING;
        }
        return null;
    }

    private boolean isHoveringUpgradeCard(int cardX, int cardY, int mouseX, int mouseY) {
        int left = screenLeft() + cardX;
        int top = screenTop() + cardY;
        return mouseX >= left && mouseX < left + UPGRADE_W && mouseY >= top && mouseY < top + UPGRADE_H;
    }

    private @Nullable CoreUpgrade hoveringUpgradeSlot(int mouseX, int mouseY) {
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            Slot slot = menu.slots.get(menu.getUpgradeSlotIndex(upgrade));
            int left = screenLeft() + slot.x;
            int top = screenTop() + slot.y;
            if (mouseX >= left - 1 && mouseX < left + 17 && mouseY >= top - 1 && mouseY < top + 17) {
                return upgrade;
            }
        }
        return null;
    }

    private void drawMagicProgress(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, boolean active) {
        int beamX = x + 10;
        int beamWidth = width - 20;
        int centerY = y + height / 2;
        int fillWidth = Math.max(0, Math.min(beamWidth, Math.round(beamWidth * progress)));

        drawMagicNode(guiGraphics, x, y + 1, active);
        drawMagicNode(guiGraphics, x + width - 8, y + 1, active);

        guiGraphics.fill(beamX, centerY - 1, beamX + beamWidth, centerY + 1, 0xFF1B222B);
        guiGraphics.fill(beamX, centerY, beamX + beamWidth, centerY + 1, 0xFF3D4757);
        if (fillWidth > 0) {
            guiGraphics.fill(beamX, centerY - 1, beamX + fillWidth, centerY + 1, COLOR_MAGIC);
            guiGraphics.fill(beamX, centerY, beamX + fillWidth, centerY + 1, COLOR_ACCENT);
        }

        for (int i = 0; i < 5; i++) {
            int runeCenter = beamX + 8 + i * 22;
            int runeColor = active && fillWidth > i * 22 ? animatedRuneColor(i) : COLOR_SOFT;
            drawRuneDiamond(guiGraphics, runeCenter, centerY, runeColor);
        }

        if (active && fillWidth > 0) {
            int tick = minecraft != null && minecraft.level != null ? (int) minecraft.level.getGameTime() : 0;
            int pulseX = beamX + Math.min(fillWidth - 1, (tick * 2) % Math.max(1, fillWidth));
            guiGraphics.fill(pulseX, centerY - 3, pulseX + 1, centerY + 4, COLOR_ACCENT);
            guiGraphics.fill(pulseX - 1, centerY - 1, pulseX + 2, centerY + 2, COLOR_OK);
        }
    }

    private int animatedRuneColor(int index) {
        int tick = minecraft != null && minecraft.level != null ? (int) minecraft.level.getGameTime() : 0;
        return (tick / 6 + index) % 2 == 0 ? COLOR_ACCENT : COLOR_OK;
    }

    private void drawMagicNode(GuiGraphics guiGraphics, int x, int y, boolean active) {
        int outer = active ? COLOR_ACCENT : COLOR_BORDER;
        int inner = active ? COLOR_OK : COLOR_SOFT;
        guiGraphics.renderOutline(x, y, 8, 8, outer);
        guiGraphics.fill(x + 2, y + 2, x + 6, y + 6, 0xFF11161D);
        guiGraphics.fill(x + 3, y + 1, x + 5, y + 7, inner);
        guiGraphics.fill(x + 1, y + 3, x + 7, y + 5, inner);
    }

    private void drawRuneDiamond(GuiGraphics guiGraphics, int centerX, int centerY, int color) {
        guiGraphics.fill(centerX, centerY - 2, centerX + 1, centerY + 3, color);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
        guiGraphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
    }

    private void setWidgetState(AbstractWidget widget, boolean visible, boolean active) {
        widget.visible = visible;
        widget.active = visible && active;
    }

    private void togglePassword() {
        passwordEnabled = !passwordEnabled;
        refreshToggleLabel();
    }

    private void toggleClearMode() {
        clearMode = clearMode.next();
        refreshClearModeLabel();
    }

    private void refreshToggleLabel() {
        passwordToggleButton.setMessage(Component.translatable(
                passwordEnabled ? "button.easyadventure.password_on" : "button.easyadventure.password_off"
        ));
    }

    private void refreshClearModeLabel() {
        clearModeButton.setMessage(Component.translatable(clearMode.translationKey()));
    }

    private void save() {
        int newX = clamp(parseValue(xEdit, 9), BaseCoreBlockEntity.MIN_SIZE_XZ, BaseCoreBlockEntity.MAX_SIZE_XZ);
        int newY = clamp(parseValue(yEdit, 5), BaseCoreBlockEntity.MIN_SIZE_Y, BaseCoreBlockEntity.MAX_SIZE_Y);
        int newBelowY = clamp(parseValue(downYEdit, 0), BaseCoreBlockEntity.MIN_SIZE_BELOW_Y, BaseCoreBlockEntity.MAX_SIZE_BELOW_Y);
        int newZ = clamp(parseValue(zEdit, 9), BaseCoreBlockEntity.MIN_SIZE_XZ, BaseCoreBlockEntity.MAX_SIZE_XZ);
        EasyAdventureNetwork.sendToServer(new UpdateCoreSizePayload(
                menu.getPos(),
                newX,
                newY,
                newBelowY,
                newZ,
                baseNameEdit.getValue(),
                clearMode,
                passwordEnabled,
                passwordEdit.getValue()
        ));
        onClose();
    }

    private void addResident() {
        ResidentCandidate candidate = selectedResidentCandidate();
        if (candidate == null) {
            return;
        }
        EasyAdventureNetwork.sendToServer(new UpdateCoreResidentPayload(menu.getPos(), BaseCoreBlockEntity.ResidentAction.ADD, candidate.name()));
    }

    private void removeResident() {
        if (selectedResidentUuid == null) {
            return;
        }
        EasyAdventureNetwork.sendToServer(new UpdateCoreResidentPayload(menu.getPos(), BaseCoreBlockEntity.ResidentAction.REMOVE, selectedResidentUuid.toString()));
        selectedResidentUuid = null;
    }

    private void changeResidentPage(int delta) {
        residentPage = Math.max(0, residentPage + delta);
        refreshDynamicState();
    }

    private void cycleResidentCandidate(int delta) {
        List<ResidentCandidate> candidates = collectResidentCandidates(currentCore());
        if (candidates.isEmpty()) {
            selectedCandidateUuid = null;
            return;
        }

        int currentIndex = 0;
        if (selectedCandidateUuid != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).uuid().equals(selectedCandidateUuid)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = Math.floorMod(currentIndex + delta, candidates.size());
        selectedCandidateUuid = candidates.get(nextIndex).uuid();
        refreshDynamicState();
    }

    private void selectResident(int index) {
        BaseCoreBlockEntity core = currentCore();
        if (core == null) {
            return;
        }

        List<CoreResident> residents = core.getResidentList();
        int residentIndex = residentPage * RESIDENTS_PER_PAGE + index;
        if (residentIndex >= 0 && residentIndex < residents.size()) {
            selectedResidentUuid = residents.get(residentIndex).uuid();
        }
    }

    private void togglePermission(CorePermission permission) {
        CoreResident resident = selectedResident(currentCore());
        if (resident == null || isSelectedResidentSelf(resident)) {
            return;
        }

        EasyAdventureNetwork.sendToServer(new UpdateResidentPermissionPayload(
                menu.getPos(),
                resident.uuid(),
                permission.ordinal(),
                !resident.hasPermission(permission)
        ));
    }

    private boolean hasNextResidentPage() {
        BaseCoreBlockEntity core = currentCore();
        if (core == null) {
            return false;
        }
        return (residentPage + 1) * RESIDENTS_PER_PAGE < core.getResidentList().size();
    }

    private boolean hasResidentCandidates() {
        return !collectResidentCandidates(currentCore()).isEmpty();
    }

    private @Nullable CoreResident selectedResident(@Nullable BaseCoreBlockEntity core) {
        if (core == null || selectedResidentUuid == null) {
            return null;
        }
        return core.getResidents().get(selectedResidentUuid);
    }

    private @Nullable ResidentCandidate selectedResidentCandidate() {
        if (selectedCandidateUuid == null) {
            return null;
        }
        return collectResidentCandidates(currentCore()).stream()
                .filter(candidate -> candidate.uuid().equals(selectedCandidateUuid))
                .findFirst()
                .orElse(null);
    }

    private Component selectedCandidateLabel() {
        ResidentCandidate candidate = selectedResidentCandidate();
        if (candidate == null) {
            return Component.translatable("gui.easyadventure.resident_hint");
        }
        return Component.literal(trimText(candidate.name(), 112));
    }

    private List<ResidentCandidate> collectResidentCandidates(@Nullable BaseCoreBlockEntity core) {
        LinkedHashMap<UUID, ResidentCandidate> candidates = new LinkedHashMap<>();

        if (minecraft != null && minecraft.getConnection() != null) {
            List<PlayerInfo> onlinePlayers = new ArrayList<>(minecraft.getConnection().getOnlinePlayers());
            onlinePlayers.sort(Comparator.comparing(
                    info -> info.getProfile().getName(),
                    String.CASE_INSENSITIVE_ORDER
            ));
            for (PlayerInfo playerInfo : onlinePlayers) {
                UUID uuid = playerInfo.getProfile().getId();
                String name = playerInfo.getProfile().getName();
                if (uuid == null || name == null || name.isBlank()) {
                    continue;
                }
                candidates.put(uuid, new ResidentCandidate(uuid, name));
            }
        }
        if (core == null) {
            return new ArrayList<>(candidates.values());
        }

        if (core.getOwnerUUID() != null) {
            candidates.remove(core.getOwnerUUID());
        }
        for (UUID residentUuid : core.getResidents().keySet()) {
            candidates.remove(residentUuid);
        }
        return new ArrayList<>(candidates.values());
    }

    private EditBox createNumberBox(int x, int y, int value, boolean editable) {
        EditBox box = new EditBox(this.font, x, y, 48, 18, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(input -> input.matches("\\d*"));
        box.setEditable(editable);
        box.setTextColor(editable ? 0xFFFFFF : 0x8E8E8E);
        return box;
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, COLOR_PANEL);
        guiGraphics.renderOutline(x, y, width, height, COLOR_BORDER);
    }

    private void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, COLOR_CARD);
        guiGraphics.renderOutline(x, y, width, height, COLOR_BORDER);
    }

    private void drawTrimmed(GuiGraphics guiGraphics, Component component, int x, int y, int maxWidth, int color) {
        guiGraphics.drawString(this.font, Component.literal(trimText(component.getString(), maxWidth)), x, y, color);
    }

    private String trimText(String text, int maxWidth) {
        if (text == null || text.isEmpty() || this.font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, available) + ellipsis;
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

    private boolean isEditingText() {
        return isFocusedEditBox(xEdit)
                || isFocusedEditBox(yEdit)
                || isFocusedEditBox(downYEdit)
                || isFocusedEditBox(zEdit)
                || isFocusedEditBox(baseNameEdit)
                || isFocusedEditBox(passwordEdit);
    }

    private static boolean isFocusedEditBox(@Nullable EditBox box) {
        return box != null && box.visible && box.isFocused();
    }

    private int screenLeft() {
        return (this.width - this.imageWidth) / 2;
    }

    private int screenTop() {
        return (this.height - this.imageHeight) / 2;
    }

    private @Nullable BaseCoreBlockEntity currentCore() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.getPos()) instanceof BaseCoreBlockEntity core ? core : null;
    }

    private boolean hasManagerAccess() {
        return minecraft != null
                && minecraft.player != null
                && currentCore() != null
                && currentCore().canPlayerManage(minecraft.player);
    }

    private boolean isSelectedResidentSelf(@Nullable CoreResident resident) {
        return resident != null
                && minecraft != null
                && minecraft.player != null
                && resident.uuid().equals(minecraft.player.getUUID());
    }

    private enum Page {
        GENERAL("button.easyadventure.page_general"),
        RESIDENTS("button.easyadventure.page_residents"),
        UPGRADES("button.easyadventure.page_upgrades");

        private final String titleKey;

        Page(String titleKey) {
            this.titleKey = titleKey;
        }

        private Component title() {
            return Component.translatable(titleKey);
        }
    }

    private record ResidentCandidate(UUID uuid, String name) {
    }
}
