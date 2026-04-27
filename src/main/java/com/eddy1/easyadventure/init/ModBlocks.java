package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.block.KeyRecallTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EasyAdventure.MODID);

    public static final RegistryObject<BaseCoreBlock> BASE_CORE = BLOCKS.register(
            "base_core",
            () -> new BaseCoreBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000.0f).noLootTable(), 9, 5, 9)
    );

    public static final RegistryObject<KeyRecallTableBlock> KEY_RECALL_TABLE = BLOCKS.register(
            "key_recall_table",
            () -> new KeyRecallTableBlock(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).noOcclusion())
    );

    private ModBlocks() {
    }
}
