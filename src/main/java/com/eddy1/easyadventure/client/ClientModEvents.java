package com.eddy1.easyadventure.client;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.client.screen.BaseNameScreen;
import com.eddy1.easyadventure.client.screen.CoreSizeScreen;
import com.eddy1.easyadventure.client.screen.KeyPasswordScreen;
import com.eddy1.easyadventure.client.screen.KeyRecallScreen;
import com.eddy1.easyadventure.init.ModMenuTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;

@EventBusSubscriber(modid = EasyAdventure.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.CORE_SIZE_MENU.get(), CoreSizeScreen::new);
            MenuScreens.register(ModMenuTypes.KEY_PASSWORD_MENU.get(), KeyPasswordScreen::new);
            MenuScreens.register(ModMenuTypes.KEY_RECALL_MENU.get(), KeyRecallScreen::new);
            MenuScreens.register(ModMenuTypes.BASE_NAME_MENU.get(), BaseNameScreen::new);
        });
    }
}
