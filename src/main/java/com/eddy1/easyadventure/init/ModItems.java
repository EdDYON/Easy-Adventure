package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.item.BaseKeyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EasyAdventure.MODID);

    public static final RegistryObject<BlockItem> BASE_CORE_ITEM = ITEMS.register(
            "base_core",
            () -> new BlockItem(ModBlocks.BASE_CORE.get(), new Item.Properties())
    );

    public static final RegistryObject<BaseKeyItem> BASE_KEY_ITEM = ITEMS.register(
            "base_key",
            () -> new BaseKeyItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
