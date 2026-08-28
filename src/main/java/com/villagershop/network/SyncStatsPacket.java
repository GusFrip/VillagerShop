package com.villagershop.network;

import com.villagershop.stats.ShopSalesStats;
import com.villagershop.stats.ShopStatSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S2C : pousse l'instantané des stats de ventes du joueur et ouvre l'écran Registre. */
public class SyncStatsPacket {
    private final List<ShopStatSnapshot> shops;

    public SyncStatsPacket(List<ShopStatSnapshot> shops) {
        this.shops = shops;
    }

    public static SyncStatsPacket of(List<ShopSalesStats.ShopRecord> records) {
        List<ShopStatSnapshot> list = new ArrayList<>(records.size());
        for (ShopSalesStats.ShopRecord r : records) list.add(ShopStatSnapshot.of(r));
        return new SyncStatsPacket(list);
    }

    public static void encode(SyncStatsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.shops.size());
        for (ShopStatSnapshot s : msg.shops) s.encode(buf);
    }

    public static SyncStatsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ShopStatSnapshot> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(ShopStatSnapshot.decode(buf));
        return new SyncStatsPacket(list);
    }

    public static void handle(SyncStatsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.villagershop.client.ClientPacketHandler.handleSyncStats(msg.shops)));
        ctx.get().setPacketHandled(true);
    }

    public List<ShopStatSnapshot> getShops() { return shops; }
}
