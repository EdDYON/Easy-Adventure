package com.eddy1.easyadventure.menu;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CoreSizeMenu extends AbstractContainerMenu {
    private static final int HOTBAR_X = 121;
    private static final int HOTBAR_Y = 194;

    private final BlockPos pos;
    private final boolean sizeLocked;
    private final boolean passwordEnabled;
    private final boolean bound;
    private final UpgradeFuelSlot[] upgradeFuelSlots = new UpgradeFuelSlot[CoreUpgrade.values().length];

    public CoreSizeMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), extraData.readBoolean(), extraData.readBoolean(), extraData.readBoolean());
    }

    public CoreSizeMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, pos, false, false, false);
    }

    public CoreSizeMenu(int containerId, Inventory inventory, BlockPos pos, boolean sizeLocked, boolean passwordEnabled, boolean bound) {
        super(ModMenuTypes.CORE_SIZE_MENU.get(), containerId);
        this.pos = pos;
        this.sizeLocked = sizeLocked;
        this.passwordEnabled = passwordEnabled;
        this.bound = bound;

        BaseCoreBlockEntity core = resolveCore(inventory, pos);
        addUpgradeFuelSlots(core);
        addPlayerInventorySlots(inventory);
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isSizeLocked() {
        return sizeLocked;
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public boolean isBound() {
        return bound;
    }

    public int getUpgradeSlotIndex(CoreUpgrade upgrade) {
        return upgrade.ordinal();
    }

    public void setUpgradeSlotsVisible(boolean visible) {
        for (UpgradeFuelSlot slot : upgradeFuelSlots) {
            if (slot != null) {
                slot.setVisible(visible);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int fuelSlotCount = CoreUpgrade.values().length;
        int playerSlotStart = fuelSlotCount;
        int playerSlotEnd = slots.size();

        if (index < fuelSlotCount) {
            if (!moveItemStackTo(original, playerSlotStart, playerSlotEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (CoreUpgrade upgrade : CoreUpgrade.values()) {
                if (!original.is(upgrade.material())) {
                    continue;
                }

                int slotIndex = getUpgradeSlotIndex(upgrade);
                if (moveItemStackTo(original, slotIndex, slotIndex + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, ModBlocks.BASE_CORE.get());
    }

    private void addUpgradeFuelSlots(BaseCoreBlockEntity core) {
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            UpgradeFuelSlot slot = new UpgradeFuelSlot(
                    core,
                    upgrade.ordinal(),
                    upgradeSlotX(upgrade),
                    upgradeSlotY(upgrade),
                    upgrade
            );
            upgradeFuelSlots[upgrade.ordinal()] = slot;
            addSlot(slot);
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
        }
    }

    private static BaseCoreBlockEntity resolveCore(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof BaseCoreBlockEntity core) {
            return core;
        }
        return new BaseCoreBlockEntity(pos, ModBlocks.BASE_CORE.get().defaultBlockState());
    }

    private static int upgradeSlotX(CoreUpgrade upgrade) {
        return switch (upgrade) {
            case BLAST_SHIELD, PURIFICATION -> 36;
            case GREENHOUSE, FOLDING -> 216;
        };
    }

    private static int upgradeSlotY(CoreUpgrade upgrade) {
        return switch (upgrade) {
            case BLAST_SHIELD, GREENHOUSE -> 78;
            case PURIFICATION, FOLDING -> 136;
        };
    }

    private static final class UpgradeFuelSlot extends Slot {
        private final CoreUpgrade upgrade;
        private boolean visible;

        private UpgradeFuelSlot(BaseCoreBlockEntity core, int slot, int x, int y, CoreUpgrade upgrade) {
            super(core, slot, x, y);
            this.upgrade = upgrade;
        }

        private void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(upgrade.material());
        }

        @Override
        public boolean isActive() {
            return visible;
        }
    }
}
