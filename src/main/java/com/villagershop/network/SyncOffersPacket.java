package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.data.ShopOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** S2C : envoie toutes les offres + l'index sélectionné pour l'affichage de la liste. */
public record SyncOffersPacket(List<ShopOffer> offers, int selected) implements CustomPacketPayload {
    public static final Type<SyncOffersPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "sync_offers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOffersPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SyncOffersPacket decode(RegistryFriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<ShopOffer> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                ItemStack a = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ItemStack b = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ItemStack r = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                list.add(new ShopOffer(a, b, r));
            }
            int sel = buf.readVarInt();
            return new SyncOffersPacket(list, sel);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, SyncOffersPacket msg) {
            buf.writeVarInt(msg.offers.size());
            for (ShopOffer o : msg.offers) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, o.getPriceA());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, o.getPriceB());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, o.getResult());
            }
            buf.writeVarInt(msg.selected);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Côté client uniquement (receveur enregistré dans ClientSetup). */
    public static void handleClient(SyncOffersPacket msg) {
        com.villagershop.client.ClientPacketHandler.handleSyncOffers(msg.offers, msg.selected);
    }
}
