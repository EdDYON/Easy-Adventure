package com.eddy1.easyadventure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record SavedBlockInfo(BlockPos relativePos, BlockState state, CompoundTag nbt) {

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", relativePos.getX());
        tag.putInt("Y", relativePos.getY());
        tag.putInt("Z", relativePos.getZ());

        // 关键：使用 NbtUtils 保存 BlockState
        tag.put("State", NbtUtils.writeBlockState(state));

        if (nbt != null) {
            tag.put("Nbt", nbt);
        }
        return tag;
    }

    public static SavedBlockInfo fromTag(CompoundTag tag) {
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));

        BlockState state = Blocks.AIR.defaultBlockState();
        if (tag.contains("State")) {
            state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("State"));
        }

        CompoundTag nbt = tag.contains("Nbt") ? tag.getCompound("Nbt") : null;

        return new SavedBlockInfo(pos, state, nbt);
    }
}