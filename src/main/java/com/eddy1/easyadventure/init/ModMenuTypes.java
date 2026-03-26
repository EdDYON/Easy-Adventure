package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import com.eddy1.easyadventure.menu.KeyPasswordMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EasyAdventure.MODID);

    public static final RegistryObject<MenuType<CoreSizeMenu>> CORE_SIZE_MENU =
            MENUS.register("core_size_menu", () -> IForgeMenuType.create(CoreSizeMenu::new));

    public static final RegistryObject<MenuType<KeyPasswordMenu>> KEY_PASSWORD_MENU =
            MENUS.register("key_password_menu", () -> IForgeMenuType.create(KeyPasswordMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
