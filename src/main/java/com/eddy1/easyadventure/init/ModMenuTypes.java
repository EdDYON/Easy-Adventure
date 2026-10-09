package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EasyAdventure.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CoreSizeMenu>> CORE_SIZE_MENU =
            MENUS.register("core_size_menu", () -> IMenuTypeExtension.create(CoreSizeMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}