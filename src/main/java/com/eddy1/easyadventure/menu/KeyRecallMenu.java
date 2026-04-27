package com.eddy1.easyadventure.menu;

import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.init.ModMenuTypes;
import com.eddy1.easyadventure.world.BaseRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeyRecallMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final List<RecallEntry> entries;

    public KeyRecallMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), readEntries(extraData));
    }

    public KeyRecallMenu(int containerId, Inventory inventory, BlockPos pos, List<RecallEntry> entries) {
        super(ModMenuTypes.KEY_RECALL_MENU.get(), containerId);
        this.pos = pos;
        this.entries = List.copyOf(entries);
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<RecallEntry> getEntries() {
        return entries;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, ModBlocks.KEY_RECALL_TABLE.get());
    }

    public static void writeEntries(FriendlyByteBuf buffer, List<BaseRegistryData.BaseRecord> records) {
        buffer.writeVarInt(records.size());
        for (BaseRegistryData.BaseRecord record : records) {
            buffer.writeUUID(record.coreUuid());
            buffer.writeUtf(record.baseName(), 64);
            buffer.writeUtf(record.state(), 16);
        }
    }

    private static List<RecallEntry> readEntries(FriendlyByteBuf buffer) {
        int count = Math.min(64, buffer.readVarInt());
        List<RecallEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new RecallEntry(buffer.readUUID(), buffer.readUtf(64), buffer.readUtf(16)));
        }
        return entries;
    }

    public record RecallEntry(UUID coreUuid, String baseName, String state) {
    }
}
