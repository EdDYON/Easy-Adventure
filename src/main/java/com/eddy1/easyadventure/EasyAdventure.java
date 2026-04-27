package com.eddy1.easyadventure;

import com.eddy1.easyadventure.command.EasyAdventureCommands;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.init.ModCreativeTabs;
import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.init.ModMenuTypes;
import com.eddy1.easyadventure.network.EasyAdventureNetwork;
import com.eddy1.easyadventure.world.TerritoryEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EasyAdventure.MODID)
public class EasyAdventure {
    public static final String MODID = "easyadventure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EasyAdventure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        EasyAdventureNetwork.register();
        TerritoryEvents.register();
        MinecraftForge.EVENT_BUS.register(EasyAdventureCommands.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
