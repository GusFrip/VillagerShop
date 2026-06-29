package com.villagershop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S historique : demandait l'état des offres. Désormais inutile (les offres se
 * synchronisent via les slots du menu). Conservé en no-op pour compat réseau.
 */
public class RequestOffersPacket {
    public RequestOffersPacket() {}

    public static void encode(RequestOffersPacket msg, FriendlyByteBuf buf) {}

    public static RequestOffersPacket decode(FriendlyByteBuf buf) {
        return new RequestOffersPacket();
    }

    public static void handle(RequestOffersPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
