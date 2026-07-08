package com.villagershop.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ModNetwork {

    /** Côté commun : types + receveurs serveur (les handlers Fabric tournent sur le thread serveur). */
    public static void register() {
        // C2S
        PayloadTypeRegistry.playC2S().register(BuyOfferPacket.TYPE, BuyOfferPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ManageAllowedPacket.TYPE, ManageAllowedPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestAllowedPacket.TYPE, RequestAllowedPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetShopNamePacket.TYPE, SetShopNamePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestOffersPacket.TYPE, RequestOffersPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(OpenSubScreenPacket.TYPE, OpenSubScreenPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(LocateVillagerPacket.TYPE, LocateVillagerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveActionPacket.TYPE, SaveActionPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TransferOwnerPacket.TYPE, TransferOwnerPacket.STREAM_CODEC);
        // S2C
        PayloadTypeRegistry.playS2C().register(SyncAllowedPacket.TYPE, SyncAllowedPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncOffersPacket.TYPE, SyncOffersPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BuyOfferPacket.TYPE, (msg, ctx) -> BuyOfferPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(ManageAllowedPacket.TYPE, (msg, ctx) -> ManageAllowedPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(RequestAllowedPacket.TYPE, (msg, ctx) -> RequestAllowedPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(SetShopNamePacket.TYPE, (msg, ctx) -> SetShopNamePacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(RequestOffersPacket.TYPE, (msg, ctx) -> RequestOffersPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(OpenSubScreenPacket.TYPE, (msg, ctx) -> OpenSubScreenPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(LocateVillagerPacket.TYPE, (msg, ctx) -> LocateVillagerPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(SaveActionPacket.TYPE, (msg, ctx) -> SaveActionPacket.handle(msg, ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(TransferOwnerPacket.TYPE, (msg, ctx) -> TransferOwnerPacket.handle(msg, ctx.player()));
    }

    @Environment(EnvType.CLIENT) // référence une classe client-only : absente du serveur dédié
    public static void sendToServer(CustomPacketPayload msg) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(msg);
    }

    public static void sendTo(CustomPacketPayload msg, ServerPlayer player) {
        ServerPlayNetworking.send(player, msg);
    }
}
