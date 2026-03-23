package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;

import java.util.Comparator;

public final class CoreLifecycle {
    private CoreLifecycle() {
    }

    public static void startResize(
            CoreRuntimeState runtime,
            BlockPos center,
            CoreVolume oldVolume,
            CoreVolume newVolume,
            boolean firstInitialization
    ) {
        runtime.begin(BaseCoreBlockEntity.State.GENERATING);
        runtime.setTotalBlocksToProcess(CoreTaskPlanner.planResize(
                runtime.taskQueue(),
                center,
                oldVolume,
                newVolume,
                firstInitialization
        ));
    }

    public static void startDeployment(CoreRuntimeState runtime, BlockPos center, CoreVolume volume) {
        queueVolume(runtime, BaseCoreBlockEntity.State.CLEARING, center, volume, true);
    }

    public static void startBuilding(CoreRuntimeState runtime, BlockPos center, CoreVolume volume) {
        queueVolume(runtime, BaseCoreBlockEntity.State.GENERATING, center, volume, true);
    }

    public static boolean startUnpacking(
            CoreRuntimeState runtime,
            BlockPos center,
            CoreStructureWorkspace workspace,
            Comparator<SavedBlockInfo> placementOrder
    ) {
        if (!workspace.hasIncomingBlocks()) {
            return false;
        }

        runtime.begin(BaseCoreBlockEntity.State.UNPACKING);
        runtime.setTotalBlocksToProcess(CoreTaskPlanner.planStructureBlocks(
                runtime.taskQueue(),
                center,
                workspace.incomingBlocks(),
                placementOrder
        ));
        return true;
    }

    public static boolean startApplyingBlockEntityData(
            CoreRuntimeState runtime,
            BlockPos center,
            CoreStructureWorkspace workspace,
            Comparator<BlockPos> relativePositionOrder
    ) {
        if (!workspace.hasPendingBlockEntityLoads()) {
            return false;
        }

        runtime.begin(BaseCoreBlockEntity.State.APPLYING_BLOCK_ENTITY_DATA);
        runtime.setTotalBlocksToProcess(CoreTaskPlanner.planRelativePositions(
                runtime.taskQueue(),
                center,
                workspace.pendingBlockEntityPositions(),
                relativePositionOrder
        ));
        return true;
    }

    public static void startRestoring(CoreRuntimeState runtime, BlockPos center, CoreVolume volume) {
        queueVolume(runtime, BaseCoreBlockEntity.State.RESTORING, center, volume, false);
    }

    public static void startPacking(CoreRuntimeState runtime, BlockPos center, CoreVolume volume) {
        queueVolume(runtime, BaseCoreBlockEntity.State.PACKING, center, volume, true);
    }

    public static void resume(
            CoreRuntimeState runtime,
            BaseCoreBlockEntity.State state,
            BlockPos center,
            CoreVolume volume,
            CoreStructureWorkspace workspace,
            Comparator<SavedBlockInfo> placementOrder,
            Comparator<BlockPos> relativePositionOrder
    ) {
        switch (state) {
            case PACKING -> startPacking(runtime, center, volume);
            case RESTORING -> startRestoring(runtime, center, volume);
            case CLEARING -> startDeployment(runtime, center, volume);
            case GENERATING -> startBuilding(runtime, center, volume);
            case UNPACKING -> startUnpacking(runtime, center, workspace, placementOrder);
            case APPLYING_BLOCK_ENTITY_DATA -> startApplyingBlockEntityData(runtime, center, workspace, relativePositionOrder);
            case IDLE -> runtime.finish();
        }
    }

    private static void queueVolume(
            CoreRuntimeState runtime,
            BaseCoreBlockEntity.State state,
            BlockPos center,
            CoreVolume volume,
            boolean topDown
    ) {
        runtime.begin(state);
        runtime.setTotalBlocksToProcess(CoreTaskPlanner.planVolume(runtime.taskQueue(), center, volume, topDown));
    }
}
