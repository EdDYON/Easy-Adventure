package com.eddy1.easyadventure.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

public record CoreVolume(int sizeX, int sizeY, int sizeBelowY, int sizeZ) {
    public CoreVolume(int sizeX, int sizeY, int sizeZ) {
        this(sizeX, sizeY, 0, sizeZ);
    }

    public int halfX() {
        return sizeX / 2;
    }

    public int halfZ() {
        return sizeZ / 2;
    }

    public int minYOffset() {
        return -sizeBelowY;
    }

    public int maxYOffset() {
        return sizeY;
    }

    public int totalHeight() {
        return sizeY + sizeBelowY + 1;
    }

    public boolean contains(BlockPos center, BlockPos pos) {
        int relativeY = pos.getY() - center.getY();
        return Math.abs(pos.getX() - center.getX()) <= halfX()
                && Math.abs(pos.getZ() - center.getZ()) <= halfZ()
                && relativeY >= minYOffset()
                && relativeY <= maxYOffset();
    }

    public void forEachPosition(BlockPos center, boolean topDown, Consumer<BlockPos> consumer) {
        if (topDown) {
            for (int y = maxYOffset(); y >= minYOffset(); y--) {
                emitLayer(center, y, consumer);
            }
            return;
        }

        for (int y = minYOffset(); y <= maxYOffset(); y++) {
            emitLayer(center, y, consumer);
        }
    }

    public AABB createAabb(BlockPos center) {
        BlockPos minPos = center.offset(-halfX(), minYOffset(), -halfZ());
        BlockPos maxPos = center.offset(halfX(), maxYOffset(), halfZ()).offset(1, 1, 1);
        return new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    private void emitLayer(BlockPos center, int y, Consumer<BlockPos> consumer) {
        for (int x = -halfX(); x <= halfX(); x++) {
            for (int z = -halfZ(); z <= halfZ(); z++) {
                BlockPos target = center.offset(x, y, z);
                if (!target.equals(center)) {
                    consumer.accept(target);
                }
            }
        }
    }
}
