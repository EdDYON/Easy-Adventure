package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.util.BlockPlacementUtil;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CoreTerrainTracker {
    private final Map<BlockPos, SavedBlockInfo> originalBlocks = new LinkedHashMap<>();

    public void clear() {
        originalBlocks.clear();
    }

    public void remember(BlockPos center, BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        BlockPos relativePos = pos.subtract(center);
        originalBlocks.putIfAbsent(relativePos, new SavedBlockInfo(relativePos, state, nbt == null ? null : nbt.copy()));
    }

    public void restore(Level level, BlockPos center, BlockPos pos) {
        BlockPos relativePos = pos.subtract(center);
        SavedBlockInfo info = originalBlocks.get(relativePos);
        BlockState restoreState = info != null ? info.state() : (relativePos.getY() <= 0 ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState());
        CompoundTag restoreNbt = info == null || info.nbt() == null ? null : info.nbt().copy();

        if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
        }
        BlockPlacementUtil.placeForRestore(level, pos, restoreState);
        BlockPlacementUtil.loadBlockEntity(level, pos, restoreNbt);
        BlockPlacementUtil.refreshNeighbors(level, pos);
    }

    public ListTag toTag() {
        ListTag terrainList = new ListTag();
        for (SavedBlockInfo info : originalBlocks.values()) {
            terrainList.add(info.toTag());
        }
        return terrainList;
    }

    public void load(ListTag terrainList) {
        clear();
        for (Tag entry : terrainList) {
            SavedBlockInfo info = SavedBlockInfo.fromTag((CompoundTag) entry);
            originalBlocks.put(info.relativePos(), info);
        }
    }
}
