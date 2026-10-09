package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EasyAdventure.MODID);

    public static final RegistryObject<BlockEntityType<BaseCoreBlockEntity>> BASE_CORE =
            BLOCK_ENTITIES.register(
                    "base_core",
                    () -> BlockEntityType.Builder.of(BaseCoreBlockEntity::new, ModBlocks.BASE_CORE.get()).build(null)
            );

    private ModBlockEntities() {
    }
}
