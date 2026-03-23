package com.eddy1.easyadventure.menu;

import com.eddy1.easyadventure.init.ModMenuTypes;
import com.eddy1.easyadventure.network.KeyOperationAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class KeyPasswordMenu extends AbstractContainerMenu {
    private final KeyOperationAction action;
    private final BlockPos pos;
    private final Direction face;
    private final InteractionHand hand;

    public KeyPasswordMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                KeyOperationAction.fromId(extraData.readVarInt()),
                extraData.readBlockPos(),
                Direction.from3DDataValue(extraData.readVarInt()),
                InteractionHand.values()[extraData.readVarInt()]
        );
    }

    public KeyPasswordMenu(int containerId, Inventory inventory, KeyOperationAction action, BlockPos pos, Direction face, InteractionHand hand) {
        super(ModMenuTypes.KEY_PASSWORD_MENU.get(), containerId);
        this.action = action;
        this.pos = pos;
        this.face = face;
        this.hand = hand;
    }

    public KeyOperationAction getAction() {
        return action;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getFace() {
        return face;
    }

    public InteractionHand getHand() {
        return hand;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
