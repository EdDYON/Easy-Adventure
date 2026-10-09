package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.BlockPos;

import java.util.LinkedList;
import java.util.Queue;

public final class CoreRuntimeState {
    private final Queue<BlockPos> taskQueue = new LinkedList<>();

    private BaseCoreBlockEntity.State currentState = BaseCoreBlockEntity.State.IDLE;
    private int soundTick;
    private int currentProcessingY;
    private int totalBlocksToProcess = 1;
    private int processedBlocks;

    public BaseCoreBlockEntity.State state() {
        return currentState;
    }

    public boolean isIdle() {
        return currentState == BaseCoreBlockEntity.State.IDLE;
    }

    public boolean isBusy() {
        return !isIdle();
    }

    public Queue<BlockPos> taskQueue() {
        return taskQueue;
    }

    public boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    public BlockPos pollTask() {
        return taskQueue.poll();
    }

    public void begin(BaseCoreBlockEntity.State nextState) {
        currentState = nextState;
        taskQueue.clear();
        processedBlocks = 0;
        soundTick = 0;
        currentProcessingY = 0;
        totalBlocksToProcess = 1;
    }

    public void finish() {
        begin(BaseCoreBlockEntity.State.IDLE);
    }

    public int incrementSoundTick() {
        return ++soundTick;
    }

    public int currentProcessingY() {
        return currentProcessingY;
    }

    public float progress() {
        return (float) processedBlocks / Math.max(1, totalBlocksToProcess);
    }

    public void markProcessed(BlockPos center, BlockPos targetPos) {
        currentProcessingY = targetPos.getY() - center.getY();
        processedBlocks++;
    }

    public void setTotalBlocksToProcess(int totalBlocksToProcess) {
        this.totalBlocksToProcess = Math.max(1, totalBlocksToProcess);
    }
}
