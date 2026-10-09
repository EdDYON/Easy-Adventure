package com.eddy1.easyadventure.datagen;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {


        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EasyAdventure.BASE_KEY_ITEM.get())
                .pattern(" I ")
                .pattern(" L ") // L = Lapis (青金石)
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('L', Items.LAPIS_LAZULI)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_lapis", has(Items.LAPIS_LAZULI))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, EasyAdventure.BASE_CORE_ITEM.get())
                .pattern("SSS")
                .pattern("CRC") // C = Chest (箱子)
                .pattern("SSS")
                .define('S', Blocks.SMOOTH_STONE)
                .define('C', Blocks.CHEST)
                .define('R', Blocks.REDSTONE_BLOCK)
                .unlockedBy("has_redstone_block", has(Blocks.REDSTONE_BLOCK))
                .save(output);
    }
}