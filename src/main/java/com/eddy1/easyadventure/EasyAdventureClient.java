package com.eddy1.easyadventure;

import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.util.KeyDataUtil;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = EasyAdventure.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EasyAdventureClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.BASE_KEY_ITEM.get(),
                ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "has_data"),
                (stack, level, entity, seed) -> KeyDataUtil.hasStoredStructure(stack) ? 1.0F : 0.0F
        ));
    }
}
