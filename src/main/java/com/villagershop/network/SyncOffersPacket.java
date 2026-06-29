package com.villagershop.network;

import com.villagershop.client.ClientPacketHandler;
import com.villagershop.data.ShopOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S2C : envoie toutes les offres + l'index sélectionné pour l'affichage de la liste. */
public class SyncOffersPacket {
    private final List<ShopOffer> offers;
    private final int selected;

    public SyncOffersPacket(List<ShopOffer> offers, int selected) {
        this.offers = offers;
        this.selected = selected;
    }

    public static void encode(SyncOffersPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.offers.size());
        for (ShopOffer o : msg.offers) {
            buf.writeItem(o.getPriceA());
            buf.writeItem(o.getPriceB());
            buf.writeItem(o.getResult());
        }
        buf.writeVarInt(msg.selected);
    }

    public static SyncOffersPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ShopOffer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new ShopOffer(buf.readItem(), buf.readItem(), buf.readItem()));
        }
        int sel = buf.readVarInt();
        return new SyncOffersPacket(list, sel);
    }

    public static void handle(SyncOffersPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSyncOffers(msg.offers, msg.selected)));
        ctx.get().setPacketHandled(true);
    }
}
