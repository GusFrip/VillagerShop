package com.villagershop.network;

import com.villagershop.ShopMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ShopMod.MOD_ID, "main"),
                () -> VERSION, VERSION::equals, VERSION::equals);

        int id = 0;
        CHANNEL.registerMessage(id++, BuyOfferPacket.class,
                BuyOfferPacket::encode, BuyOfferPacket::decode, BuyOfferPacket::handle);
        CHANNEL.registerMessage(id++, ManageAllowedPacket.class,
                ManageAllowedPacket::encode, ManageAllowedPacket::decode, ManageAllowedPacket::handle);
        CHANNEL.registerMessage(id++, RequestAllowedPacket.class,
                RequestAllowedPacket::encode, RequestAllowedPacket::decode, RequestAllowedPacket::handle);
        CHANNEL.registerMessage(id++, SyncAllowedPacket.class,
                SyncAllowedPacket::encode, SyncAllowedPacket::decode, SyncAllowedPacket::handle);
        CHANNEL.registerMessage(id++, SetShopNamePacket.class,
                SetShopNamePacket::encode, SetShopNamePacket::decode, SetShopNamePacket::handle);
        CHANNEL.registerMessage(id++, SyncOffersPacket.class,
                SyncOffersPacket::encode, SyncOffersPacket::decode, SyncOffersPacket::handle);
        CHANNEL.registerMessage(id++, RequestOffersPacket.class,
                RequestOffersPacket::encode, RequestOffersPacket::decode, RequestOffersPacket::handle);
        CHANNEL.registerMessage(id++, OpenSubScreenPacket.class,
                OpenSubScreenPacket::encode, OpenSubScreenPacket::decode, OpenSubScreenPacket::handle);
        CHANNEL.registerMessage(id++, LocateVillagerPacket.class,
                LocateVillagerPacket::encode, LocateVillagerPacket::decode, LocateVillagerPacket::handle);
        CHANNEL.registerMessage(id++, ExportConfigPacket.class,
                ExportConfigPacket::encode, ExportConfigPacket::decode, ExportConfigPacket::handle);
        CHANNEL.registerMessage(id++, ImportConfigPacket.class,
                ImportConfigPacket::encode, ImportConfigPacket::decode, ImportConfigPacket::handle);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendTo(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
