package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.util.BlockPlacementUtil;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class CorePhaseProcessor {
    private CorePhaseProcessor() {
    }

    public static void processGenerating(
            Level level,
            BlockPos center,
            CoreVolume volume,
            CoreTerrainTracker terrainTracker,
            BlockPos pos,
            Function<BlockPos, @Nullable CompoundTag> captureBlockEntityData
    ) {
        int relativeY = pos.getY() - center.getY();
        if (!volume.contains(center, pos)) {
            terrainTracker.restore(level, center, pos);
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0) {
            return;
        }

        if (relativeY == 0) {
            terrainTracker.remember(center, pos, state, captureBlockEntityData.apply(pos));
            if (!state.is(Blocks.COBBLESTONE)) {
                BlockPlacementUtil.replaceForCapture(level, pos, Blocks.COBBLESTONE.defaultBlockState());
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                }
            }
            return;
        }

        if (!state.isAir()) {
            terrainTracker.remember(center, pos, state, captureBlockEntityData.apply(pos));
            BlockPlacementUtil.clearForCapture(level, pos);
        }
    }

    public static void processClearing(
            Level level,
            BlockPos center,
            CoreTerrainTracker terrainTracker,
            BlockPos pos,
            Function<BlockPos, @Nullable CompoundTag> captureBlockEntityData
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0 || state.isAir()) {
            return;
        }

        terrainTracker.remember(center, pos, state, captureBlockEntityData.apply(pos));
        BlockPlacementUtil.clearForCapture(level, pos);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0.05);
        }
    }

    public static void processPacking(
            Level level,
            BlockPos center,
            CoreStructureWorkspace workspace,
            BlockPos pos,
            Function<BlockPos, @Nullable CompoundTag> captureBlockEntityData
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || state.getBlock() instanceof BaseCoreBlock
                || state.is(CoreCompat.CANNOT_PACK)
                || state.getDestroySpeed(level, pos) < 0) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 coreCenter = new Vec3(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3 direction = coreCenter.subtract(blockCenter).normalize().scale(0.5);
            if (level.random.nextFloat() < 0.3F) {
                serverLevel.sendParticles(ParticleTypes.WITCH, blockCenter.x, blockCenter.y, blockCenter.z, 0, direction.x, direction.y, direction.z, 1.0);
            }
        }

        BlockPos relativePos = pos.subtract(center);
        workspace.rememberPackedBlock(new SavedBlockInfo(relativePos, state, captureBlockEntityData.apply(pos)));
        BlockPlacementUtil.clearForCapture(level, pos);
    }

    public static void processUnpacking(Level level, BlockPos center, CoreStructureWorkspace workspace, BlockPos targetPos) {
        SavedBlockInfo info = workspace.findIncomingBlock(targetPos.subtract(center));
        if (info == null) {
            return;
        }

        BlockPlacementUtil.placeForRestore(level, targetPos, info.state());
        if (info.nbt() != null) {
            workspace.queuePendingBlockEntityLoad(info.relativePos(), info.nbt());
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 1, 0, 0, 0, 0.01);
            if (level.random.nextFloat() < 0.15F) {
                serverLevel.playSound(null, targetPos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
        }
    }

    public static void processApplyingBlockEntityData(Level level, BlockPos center, CoreStructureWorkspace workspace, BlockPos targetPos) {
        BlockPos relativePos = targetPos.subtract(center);
        CompoundTag tag = workspace.removePendingBlockEntityLoad(relativePos);
        BlockPlacementUtil.loadBlockEntity(level, targetPos, tag);
    }
}
