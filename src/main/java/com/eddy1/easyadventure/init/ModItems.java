package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.item.BaseKeyItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, EasyAdventure.MODID);

    public static final DeferredHolder<Item, BlockItem> BASE_CORE_ITEM = ITEMS.register(
            "base_core",
            () -> new BlockItem(ModBlocks.BASE_CORE.get(), new Item.Properties())
    );

    public static final DeferredHolder<Item, BaseKeyItem> BASE_KEY_ITEM = ITEMS.register(
            "base_key",
            () -> new BaseKeyItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
