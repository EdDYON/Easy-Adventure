package com.eddy1.easyadventure.menu;

import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class CoreSizeMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final boolean sizeLocked;
    private final boolean passwordEnabled;

    public CoreSizeMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), extraData.readBoolean(), extraData.readBoolean());
    }

    public CoreSizeMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, pos, false, false);
    }

    public CoreSizeMenu(int containerId, Inventory inventory, BlockPos pos, boolean sizeLocked, boolean passwordEnabled) {
        super(ModMenuTypes.CORE_SIZE_MENU.get(), containerId);
        this.pos = pos;
        this.sizeLocked = sizeLocked;
        this.passwordEnabled = passwordEnabled;
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, ModBlocks.BASE_CORE.get());
    }
}
