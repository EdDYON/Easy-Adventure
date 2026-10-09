package com.eddy1.easyadventure.init;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EasyAdventure.MODID);

    public static final RegistryObject<CreativeModeTab> EASY_ADVENTURE_TAB =
            CREATIVE_MODE_TABS.register("easy_adventure_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.easyadventure"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.BASE_KEY_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BASE_KEY_ITEM.get());
                        output.accept(ModItems.PREFLIGHT_SCROLL_ITEM.get());
                        output.accept(ModItems.BASE_ARCHIVE_BOOK_ITEM.get());
                        output.accept(ModItems.BASE_CORE_ITEM.get());
                        output.accept(ModItems.BOUNDARY_MARKER_ITEM.get());
                        output.accept(ModItems.KEY_RECALL_TABLE_ITEM.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
