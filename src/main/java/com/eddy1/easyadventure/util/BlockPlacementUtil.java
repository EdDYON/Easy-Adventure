package com.eddy1.easyadventure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BlockPlacementUtil {
    public static final int CAPTURE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final int TERRAIN_RESTORE_FLAGS = Block.UPDATE_ALL;

    private BlockPlacementUtil() {
    }

    public static void clearForCapture(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), CAPTURE_FLAGS);
    }

    public static void replaceForCapture(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, CAPTURE_FLAGS);
    }

    public static void placeForRestore(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, RESTORE_FLAGS);
    }

    public static void placeForTerrainRestore(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, TERRAIN_RESTORE_FLAGS);
    }

    public static void loadBlockEntity(Level level, BlockPos pos, @Nullable CompoundTag tag) {
        if (tag == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }

        CompoundTag relocated = tag.copy();
        relocated.putInt("x", pos.getX());
        relocated.putInt("y", pos.getY());
        relocated.putInt("z", pos.getZ());
        blockEntity.load(relocated);
        blockEntity.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    public static void refreshNeighbors(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.blockUpdated(pos, state.getBlock());
        level.updateNeighborsAt(pos, state.getBlock());
    }
}
