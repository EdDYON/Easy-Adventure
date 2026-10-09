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

public class BaseNameMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final String currentName;

    public BaseNameMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), extraData.readUtf(64));
    }

    public BaseNameMenu(int containerId, Inventory inventory, BlockPos pos, String currentName) {
        super(ModMenuTypes.BASE_NAME_MENU.get(), containerId);
        this.pos = pos;
        this.currentName = currentName;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getCurrentName() {
        return currentName;
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
