package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.item.BaseArchiveBookItem;
import com.eddy1.easyadventure.item.BaseKeyItem;
import com.eddy1.easyadventure.item.PreflightScrollItem;
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

    public static final RegistryObject<BlockItem> KEY_RECALL_TABLE_ITEM = ITEMS.register(
            "key_recall_table",
            () -> new BlockItem(ModBlocks.KEY_RECALL_TABLE.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockItem> BOUNDARY_MARKER_ITEM = ITEMS.register(
            "boundary_marker",
            () -> new BlockItem(ModBlocks.BOUNDARY_MARKER.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> BLANK_BASE_SCROLL_ITEM = ITEMS.register(
            "blank_base_scroll",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<PreflightScrollItem> PREFLIGHT_SCROLL_ITEM = ITEMS.register(
            "preflight_scroll",
            () -> new PreflightScrollItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<BaseArchiveBookItem> BASE_ARCHIVE_BOOK_ITEM = ITEMS.register(
            "base_archive_book",
            () -> new BaseArchiveBookItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<BaseKeyItem> BASE_KEY_ITEM = ITEMS.register(
            "base_key",
            () -> new BaseKeyItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
