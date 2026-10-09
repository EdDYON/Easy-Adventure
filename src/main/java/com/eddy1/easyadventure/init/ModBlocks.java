package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, EasyAdventure.MODID);

    public static final DeferredHolder<Block, BaseCoreBlock> BASE_CORE = BLOCKS.register(
            "base_core",
            () -> new BaseCoreBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000.0f).noLootTable(), 9, 5, 9)
    );

    private ModBlocks() {
    }
}
