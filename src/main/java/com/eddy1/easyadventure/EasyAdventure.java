package com.eddy1.easyadventure;

import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.init.ModCreativeTabs;
import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.init.ModMenuTypes;
import com.eddy1.easyadventure.network.SubmitKeyPasswordPayload;
import com.eddy1.easyadventure.network.UpdateCoreResidentPayload;
import com.eddy1.easyadventure.network.UpdateCoreSizePayload;
import com.eddy1.easyadventure.network.UpdateResidentPermissionPayload;
import com.eddy1.easyadventure.world.TerritoryEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(EasyAdventure.MODID)
public class EasyAdventure {
    public static final String MODID = "easyadventure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EasyAdventure(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        TerritoryEvents.register();
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                UpdateCoreSizePayload.TYPE,
                UpdateCoreSizePayload.STREAM_CODEC,
                UpdateCoreSizePayload::handle
        );
        registrar.playBidirectional(
                SubmitKeyPasswordPayload.TYPE,
                SubmitKeyPasswordPayload.STREAM_CODEC,
                SubmitKeyPasswordPayload::handle
        );
        registrar.playBidirectional(
                UpdateCoreResidentPayload.TYPE,
                UpdateCoreResidentPayload.STREAM_CODEC,
                UpdateCoreResidentPayload::handle
        );
        registrar.playBidirectional(
                UpdateResidentPermissionPayload.TYPE,
                UpdateResidentPermissionPayload.STREAM_CODEC,
                UpdateResidentPermissionPayload::handle
        );
    }
}
