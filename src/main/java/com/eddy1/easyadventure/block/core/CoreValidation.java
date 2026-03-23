package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CoreValidation {
    private CoreValidation() {
    }

    public static @Nullable BlockPos findNestedCore(Level level, BlockPos center, CoreVolume volume) {
        BlockPos[] found = new BlockPos[1];
        volume.forEachPosition(center, true, pos -> {
            if (found[0] != null) {
                return;
            }
            if (level.getBlockState(pos).getBlock() instanceof BaseCoreBlock) {
                found[0] = pos;
            }
        });
        return found[0];
    }

    public static boolean containsNestedCore(StructureSnapshot snapshot) {
        for (SavedBlockInfo block : snapshot.blocks()) {
            if (block.state().getBlock() instanceof BaseCoreBlock) {
                return true;
            }
        }
        return false;
    }
}
