package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class CoreCompat {
    public static final TagKey<Block> CANNOT_PACK = blockTag("cannot_pack");
    public static final TagKey<Block> DEPLOYMENT_BLOCKERS = blockTag("deployment_blockers");
    public static final TagKey<Block> TERRITORY_STORAGE_BLOCKS = blockTag("territory_storage_blocks");
    public static final TagKey<Block> TERRITORY_DEVICE_BLOCKS = blockTag("territory_device_blocks");
    public static final TagKey<EntityType<?>> SKIP_ENTITY_CAPTURE = entityTag("skip_entity_capture");
    public static final TagKey<Item> TERRITORY_BUILD_ITEMS = itemTag("territory_build_items");

    private CoreCompat() {
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, EasyAdventure.id(path));
    }

    private static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, EasyAdventure.id(path));
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, EasyAdventure.id(path));
    }
}
