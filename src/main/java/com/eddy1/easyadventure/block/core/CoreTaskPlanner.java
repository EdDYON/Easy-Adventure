package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Comparator;
import java.util.Queue;

public final class CoreTaskPlanner {
    private CoreTaskPlanner() {
    }

    public static int planVolume(Queue<BlockPos> queue, BlockPos center, CoreVolume volume, boolean topDown) {
        volume.forEachPosition(center, topDown, queue::add);
        return Math.max(1, queue.size());
    }

    public static int planStructureBlocks(
            Queue<BlockPos> queue,
            BlockPos center,
            Collection<SavedBlockInfo> blocks,
            Comparator<SavedBlockInfo> comparator
    ) {
        blocks.stream()
                .sorted(comparator)
                .forEach(info -> queue.add(center.offset(info.relativePos())));
        return Math.max(1, queue.size());
    }

    public static int planRelativePositions(
            Queue<BlockPos> queue,
            BlockPos center,
            Collection<BlockPos> relativePositions,
            Comparator<BlockPos> comparator
    ) {
        relativePositions.stream()
                .sorted(comparator)
                .forEach(relativePos -> queue.add(center.offset(relativePos)));
        return Math.max(1, queue.size());
    }

    public static int planResize(
            Queue<BlockPos> queue,
            BlockPos center,
            CoreVolume oldVolume,
            CoreVolume newVolume,
            boolean firstInitialization
    ) {
        if (!firstInitialization) {
            for (int y = oldVolume.maxYOffset(); y >= oldVolume.minYOffset(); y--) {
                for (int x = -oldVolume.halfX(); x <= oldVolume.halfX(); x++) {
                    for (int z = -oldVolume.halfZ(); z <= oldVolume.halfZ(); z++) {
                        boolean insideNewBounds = (x >= -newVolume.halfX() && x <= newVolume.halfX())
                                && (z >= -newVolume.halfZ() && z <= newVolume.halfZ())
                                && y >= newVolume.minYOffset()
                                && y <= newVolume.maxYOffset();
                        if (!insideNewBounds) {
                            BlockPos target = center.offset(x, y, z);
                            if (!target.equals(center)) {
                                queue.add(target);
                            }
                        }
                    }
                }
            }
        }

        // Negative Y is part of the captured volume, but resizing the core should not dig out basements.
        int generationMinY = Math.max(0, newVolume.minYOffset());
        for (int x = -newVolume.halfX(); x <= newVolume.halfX(); x++) {
            for (int z = -newVolume.halfZ(); z <= newVolume.halfZ(); z++) {
                boolean oldColumn = (x >= -oldVolume.halfX() && x <= oldVolume.halfX())
                        && (z >= -oldVolume.halfZ() && z <= oldVolume.halfZ());
                for (int y = newVolume.maxYOffset(); y >= generationMinY; y--) {
                    if (!firstInitialization
                            && oldColumn
                            && y >= oldVolume.minYOffset()
                            && y <= oldVolume.maxYOffset()) {
                        continue;
                    }
                    BlockPos target = center.offset(x, y, z);
                    if (!target.equals(center)) {
                        queue.add(target);
                    }
                }
            }
        }

        return Math.max(1, queue.size());
    }
}
