package com.villagershop.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        // C2S
        registrar.playToServer(BuyOfferPacket.TYPE, BuyOfferPacket.STREAM_CODEC, BuyOfferPacket::handle);
        registrar.playToServer(ManageAllowedPacket.TYPE, ManageAllowedPacket.STREAM_CODEC, ManageAllowedPacket::handle);
        registrar.playToServer(RequestAllowedPacket.TYPE, RequestAllowedPacket.STREAM_CODEC, RequestAllowedPacket::handle);
        registrar.playToServer(SetShopNamePacket.TYPE, SetShopNamePacket.STREAM_CODEC, SetShopNamePacket::handle);
        registrar.playToServer(RequestOffersPacket.TYPE, RequestOffersPacket.STREAM_CODEC, RequestOffersPacket::handle);
        registrar.playToServer(OpenSubScreenPacket.TYPE, OpenSubScreenPacket.STREAM_CODEC, OpenSubScreenPacket::handle);
        registrar.playToServer(LocateVillagerPacket.TYPE, LocateVillagerPacket.STREAM_CODEC, LocateVillagerPacket::handle);
        registrar.playToServer(SaveActionPacket.TYPE, SaveActionPacket.STREAM_CODEC, SaveActionPacket::handle);
        registrar.playToServer(TransferOwnerPacket.TYPE, TransferOwnerPacket.STREAM_CODEC, TransferOwnerPacket::handle);

        // S2C
        registrar.playToClient(SyncAllowedPacket.TYPE, SyncAllowedPacket.STREAM_CODEC, SyncAllowedPacket::handle);
        registrar.playToClient(SyncOffersPacket.TYPE, SyncOffersPacket.STREAM_CODEC, SyncOffersPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload msg) {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendTo(CustomPacketPayload msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }
}
