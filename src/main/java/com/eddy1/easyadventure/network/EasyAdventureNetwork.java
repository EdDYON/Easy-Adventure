package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EasyAdventureNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(EasyAdventure.id("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean registered;

    private EasyAdventureNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        int id = 0;
        CHANNEL.messageBuilder(UpdateCoreSizePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateCoreSizePayload::encode)
                .decoder(UpdateCoreSizePayload::decode)
                .consumerMainThread(UpdateCoreSizePayload::handle)
                .add();
        CHANNEL.messageBuilder(SubmitKeyPasswordPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitKeyPasswordPayload::encode)
                .decoder(SubmitKeyPasswordPayload::decode)
                .consumerMainThread(SubmitKeyPasswordPayload::handle)
                .add();
        CHANNEL.messageBuilder(UpdateCoreResidentPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateCoreResidentPayload::encode)
                .decoder(UpdateCoreResidentPayload::decode)
                .consumerMainThread(UpdateCoreResidentPayload::handle)
                .add();
        CHANNEL.messageBuilder(UpdateResidentPermissionPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateResidentPermissionPayload::encode)
                .decoder(UpdateResidentPermissionPayload::decode)
                .consumerMainThread(UpdateResidentPermissionPayload::handle)
                .add();
        CHANNEL.messageBuilder(RecallBasePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RecallBasePayload::encode)
                .decoder(RecallBasePayload::decode)
                .consumerMainThread(RecallBasePayload::handle)
                .add();
        CHANNEL.messageBuilder(SubmitBaseNamePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitBaseNamePayload::encode)
                .decoder(SubmitBaseNamePayload::decode)
                .consumerMainThread(SubmitBaseNamePayload::handle)
                .add();
    }

    public static <T> void sendToServer(T message) {
        CHANNEL.sendToServer(message);
    }
}
