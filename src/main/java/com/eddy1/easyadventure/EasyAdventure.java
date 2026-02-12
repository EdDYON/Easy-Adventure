package com.eddy1.easyadventure;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.init.ModMenuTypes;
import com.eddy1.easyadventure.item.BaseKeyItem;
import com.eddy1.easyadventure.network.UpdateCoreSizePayload;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(EasyAdventure.MODID)
public class EasyAdventure {

    public static final String MODID = "easyadventure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Block, BaseCoreBlock> BASE_CORE = BLOCKS.register("base_core",
            () -> new BaseCoreBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noLootTable(), 9, 5, 9));

    public static final DeferredHolder<Item, BlockItem> BASE_CORE_ITEM = ITEMS.register("base_core",
            () -> new BlockItem(BASE_CORE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BaseKeyItem> BASE_KEY_ITEM = ITEMS.register("base_key",
            () -> new BaseKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EASY_ADVENTURE_TAB = CREATIVE_MODE_TABS.register("easy_adventure_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.easyadventure"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> BASE_KEY_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BASE_KEY_ITEM.get());
                        output.accept(BASE_CORE_ITEM.get());
                    }).build());

    public EasyAdventure(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                UpdateCoreSizePayload.TYPE,
                UpdateCoreSizePayload.STREAM_CODEC,
                UpdateCoreSizePayload::handle
        );
    }
}