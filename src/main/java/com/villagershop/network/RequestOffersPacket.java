package com.villagershop.network;

import com.villagershop.ShopMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S historique : demandait l'état des offres. Désormais inutile (les offres se
 * synchronisent via les slots du menu). Conservé en no-op pour compat réseau.
 */
public record RequestOffersPacket() implements CustomPacketPayload {
    public static final Type<RequestOffersPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "request_offers"));

    public static final StreamCodec<FriendlyByteBuf, RequestOffersPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public RequestOffersPacket decode(FriendlyByteBuf buf) { return new RequestOffersPacket(); }
        @Override public void encode(FriendlyByteBuf buf, RequestOffersPacket msg) {}
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestOffersPacket msg, IPayloadContext ctx) {}
}
