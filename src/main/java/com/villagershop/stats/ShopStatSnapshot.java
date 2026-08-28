package com.villagershop.stats;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instantané réseau des stats lifetime d'une boutique, envoyé au client pour
 * l'écran Registre. (Les buckets journaliers restent côté serveur : ils ne
 * seront synchronisés que lorsque les courbes arriveront.)
 */
public class ShopStatSnapshot {
    public final String name;
    public final String dim;
    public final BlockPos pos;
    public final boolean removed;
    /** itemId → [vendus, encaissés] */
    public final Map<String, long[]> lifetime;

    public ShopStatSnapshot(String name, String dim, BlockPos pos, boolean removed, Map<String, long[]> lifetime) {
        this.name = name;
        this.dim = dim;
        this.pos = pos;
        this.removed = removed;
        this.lifetime = lifetime;
    }

    public static ShopStatSnapshot of(ShopSalesStats.ShopRecord r) {
        Map<String, long[]> map = new LinkedHashMap<>();
        for (Map.Entry<String, ShopSalesStats.Counts> e : r.lifetime.entrySet()) {
            map.put(e.getKey(), new long[]{e.getValue().sold, e.getValue().received});
        }
        return new ShopStatSnapshot(r.name, r.dim, r.pos, r.removed, map);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(dim);
        buf.writeBlockPos(pos);
        buf.writeBoolean(removed);
        buf.writeVarInt(lifetime.size());
        for (Map.Entry<String, long[]> e : lifetime.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarLong(e.getValue()[0]);
            buf.writeVarLong(e.getValue()[1]);
        }
    }

    public static ShopStatSnapshot decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String dim = buf.readUtf();
        BlockPos pos = buf.readBlockPos();
        boolean removed = buf.readBoolean();
        int size = buf.readVarInt();
        Map<String, long[]> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            long sold = buf.readVarLong();
            long received = buf.readVarLong();
            map.put(id, new long[]{sold, received});
        }
        return new ShopStatSnapshot(name, dim, pos, removed, map);
    }
}
