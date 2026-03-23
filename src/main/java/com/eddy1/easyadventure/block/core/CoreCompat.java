package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class CoreCompat {
    public static final TagKey<Block> CANNOT_PACK = blockTag("cannot_pack");
    public static final TagKey<Block> DEPLOYMENT_BLOCKERS = blockTag("deployment_blockers");
    public static final TagKey<EntityType<?>> SKIP_ENTITY_CAPTURE = entityTag("skip_entity_capture");

    private CoreCompat() {
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, path));
    }

    private static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, path));
    }
}
