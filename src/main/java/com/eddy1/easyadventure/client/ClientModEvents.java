package com.eddy1.easyadventure.client;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.client.screen.CoreSizeScreen;
import com.eddy1.easyadventure.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = EasyAdventure.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CORE_SIZE_MENU.get(), CoreSizeScreen::new);
    }
}