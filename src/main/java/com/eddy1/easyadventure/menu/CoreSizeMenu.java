package com.eddy1.easyadventure.menu;

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

    public CoreSizeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    public CoreSizeMenu(int containerId, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.CORE_SIZE_MENU.get(), containerId);
        this.pos = pos;
    }

    public BlockPos getPos() { return pos; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(player.level(), pos).evaluate((level, p) -> true, true);
    }
}