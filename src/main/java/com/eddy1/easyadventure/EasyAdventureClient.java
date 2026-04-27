package com.eddy1.easyadventure;

import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.util.KeyDataUtil;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = EasyAdventure.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EasyAdventureClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.BASE_KEY_ITEM.get(),
                    EasyAdventure.id("has_data"),
                    (stack, level, entity, seed) -> KeyDataUtil.hasStoredStructure(stack) ? 1.0F : 0.0F
            );
            ItemProperties.register(
                    ModItems.BASE_KEY_ITEM.get(),
                    EasyAdventure.id("obsolete"),
                    (stack, level, entity, seed) -> KeyDataUtil.isObsoleteKey(stack) ? 1.0F : 0.0F
            );
        });
    }
}
