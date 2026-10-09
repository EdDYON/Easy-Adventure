package com.eddy1.easyadventure.block.core;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum CoreFoundationMaterial {
    COBBLESTONE("gui.easyadventure.foundation_cobblestone", Blocks.COBBLESTONE.defaultBlockState()),
    DIRT("gui.easyadventure.foundation_dirt", Blocks.DIRT.defaultBlockState()),
    SAND("gui.easyadventure.foundation_sand", Blocks.SAND.defaultBlockState()),
    STONE("gui.easyadventure.foundation_stone", Blocks.STONE.defaultBlockState()),
    OAK_PLANKS("gui.easyadventure.foundation_oak_planks", Blocks.OAK_PLANKS.defaultBlockState());

    private final String translationKey;
    private final BlockState blockState;

    CoreFoundationMaterial(String translationKey, BlockState blockState) {
        this.translationKey = translationKey;
        this.blockState = blockState;
    }

    public String translationKey() {
        return translationKey;
    }

    public BlockState blockState() {
        return blockState;
    }

    public CoreFoundationMaterial next() {
        CoreFoundationMaterial[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static CoreFoundationMaterial fromName(String name) {
        for (CoreFoundationMaterial material : values()) {
            if (material.name().equalsIgnoreCase(name)) {
                return material;
            }
        }
        return COBBLESTONE;
    }
}
