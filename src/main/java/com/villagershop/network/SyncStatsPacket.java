package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.stats.ShopSalesStats;
import com.villagershop.stats.ShopStatSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** S2C : pousse l'instantané des stats de ventes du joueur et ouvre l'écran Registre. */
public record SyncStatsPacket(List<ShopStatSnapshot> shops) implements CustomPacketPayload {
    public static final Type<SyncStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "sync_stats"));

    public static final StreamCodec<FriendlyByteBuf, SyncStatsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SyncStatsPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<ShopStatSnapshot> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(ShopStatSnapshot.decode(buf));
            return new SyncStatsPacket(list);
        }
        @Override public void encode(FriendlyByteBuf buf, SyncStatsPacket msg) {
            buf.writeVarInt(msg.shops.size());
            for (ShopStatSnapshot s : msg.shops) s.encode(buf);
        }
    };

    public static SyncStatsPacket of(List<ShopSalesStats.ShopRecord> records) {
        List<ShopStatSnapshot> list = new ArrayList<>(records.size());
        for (ShopSalesStats.ShopRecord r : records) list.add(ShopStatSnapshot.of(r));
        return new SyncStatsPacket(list);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncStatsPacket msg, IPayloadContext ctx) {
        // Enregistré en playToClient : ce handler ne tourne que côté client.
        com.villagershop.client.ClientPacketHandler.handleSyncStats(msg.shops);
    }
}
