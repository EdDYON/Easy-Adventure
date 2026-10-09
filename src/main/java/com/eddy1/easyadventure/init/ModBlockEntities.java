package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, EasyAdventure.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaseCoreBlockEntity>> BASE_CORE =
            BLOCK_ENTITIES.register("base_core", () ->
                    BlockEntityType.Builder.of(BaseCoreBlockEntity::new, EasyAdventure.BASE_CORE.get()).build(null));
}