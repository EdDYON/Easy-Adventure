package com.eddy1.easyadventure.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record CoreAreaCheckResult(
        boolean ok,
        int totalPositions,
        int occupiedBlocks,
        int blockedBlocks,
        @Nullable BlockPos blockedPos,
        @Nullable Component reason
) {
}
