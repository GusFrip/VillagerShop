package com.villagershop.stats;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Comptabilité des ventes des boutiques équipées de l'upgrade Communication.
 *
 * Trois niveaux de données par boutique :
 *  - lifetime  : totaux cumulés par item (vendu / encaissé) — affichés par le Registre.
 *  - dailyMc   : buckets par jour Minecraft (dayTime/24000) — réservés aux futures courbes.
 *  - dailyReal : buckets par jour réel (epoch UTC) — réservés aux futures courbes.
 *
 * La rétention des buckets est purgée paresseusement à l'écriture (pas de ticker).
 * Une boutique apparaît dès que son upgrade Communication est installée (touch),
 * même sans vente ; elle est marquée "removed" quand le comptoir est cassé.
 */
public class ShopSalesStats extends SavedData {

    private static final String NAME = "villagershop_sales_stats";
    /** Nombre de buckets conservés par type de jour. */
    public static final int RETENTION = 35;

    /** Compteurs d'un item : sold = sortis du stock, received = encaissés. */
    public static class Counts {
        public long sold;
        public long received;
    }

    /** Fiche d'une boutique. */
    public static class ShopRecord {
        public String dim = "";
        public BlockPos pos = BlockPos.ZERO;
        public String name = "Shop";
        public boolean removed = false;
        public final Set<UUID> viewers = new HashSet<>();
        public final Map<String, Counts> lifetime = new HashMap<>();
        public final Map<Long, Map<String, Counts>> dailyMc = new HashMap<>();
        public final Map<Long, Map<String, Counts>> dailyReal = new HashMap<>();
    }

    private final Map<String, ShopRecord> shops = new HashMap<>();

    public static ShopSalesStats get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(ShopSalesStats::load, ShopSalesStats::new, NAME);
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    // ----- Écriture ---------------------------------------------------------

    /** Crée/actualise la fiche d'une boutique (upgrade posée, nom changé…) sans toucher aux compteurs. */
    public static void touch(ServerLevel level, ShopBlockEntity be) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ShopSalesStats data = get(server);
        ShopRecord r = data.recordOf(level, be);
        r.removed = false;
        data.setDirty();
    }

    /** Marque la boutique comme retirée (comptoir cassé) ; l'historique est conservé. */
    public static void markRemoved(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ShopSalesStats data = get(server);
        ShopRecord r = data.shops.get(key(level, pos));
        if (r != null) {
            r.removed = true;
            data.setDirty();
        }
    }

    /** Comptabilise une transaction : marchandise sortie + paiements encaissés. */
    public static void recordTrade(ServerLevel level, ShopBlockEntity be,
                                   ItemStack sold, List<ItemStack> received) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ShopSalesStats data = get(server);
        ShopRecord r = data.recordOf(level, be);

        long mcDay = level.getDayTime() / 24000L;
        long realDay = System.currentTimeMillis() / 86_400_000L;

        if (!sold.isEmpty()) {
            String id = itemId(sold);
            bump(r.lifetime, id, sold.getCount(), 0);
            bump(r.dailyMc.computeIfAbsent(mcDay, k -> new HashMap<>()), id, sold.getCount(), 0);
            bump(r.dailyReal.computeIfAbsent(realDay, k -> new HashMap<>()), id, sold.getCount(), 0);
        }
        for (ItemStack pay : received) {
            if (pay.isEmpty()) continue;
            String id = itemId(pay);
            bump(r.lifetime, id, 0, pay.getCount());
            bump(r.dailyMc.computeIfAbsent(mcDay, k -> new HashMap<>()), id, 0, pay.getCount());
            bump(r.dailyReal.computeIfAbsent(realDay, k -> new HashMap<>()), id, 0, pay.getCount());
        }

        prune(r.dailyMc, mcDay);
        prune(r.dailyReal, realDay);
        data.setDirty();
    }

    private ShopRecord recordOf(ServerLevel level, ShopBlockEntity be) {
        ShopRecord r = shops.computeIfAbsent(key(level, be.getBlockPos()), k -> new ShopRecord());
        r.dim = level.dimension().location().toString();
        r.pos = be.getBlockPos();
        r.name = be.getShopNameOrDefault();
        r.viewers.clear();
        r.viewers.addAll(be.getOwnersAndCoowners());
        return r;
    }

    private static void bump(Map<String, Counts> map, String id, int sold, int received) {
        Counts c = map.computeIfAbsent(id, k -> new Counts());
        c.sold += sold;
        c.received += received;
    }

    private static void prune(Map<Long, Map<String, Counts>> buckets, long today) {
        buckets.keySet().removeIf(day -> day < today - RETENTION);
    }

    private static String itemId(ItemStack stack) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    // ----- Lecture ----------------------------------------------------------

    /** Fiches visibles par un joueur (owner ou co-proprio au dernier pointage). */
    public List<ShopRecord> shopsFor(UUID player) {
        List<ShopRecord> list = new ArrayList<>();
        for (ShopRecord r : shops.values()) {
            if (r.viewers.contains(player)) list.add(r);
        }
        return list;
    }

    // ----- NBT --------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, ShopRecord> e : shops.entrySet()) {
            ShopRecord r = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putString("Key", e.getKey());
            c.putString("Dim", r.dim);
            c.putInt("X", r.pos.getX());
            c.putInt("Y", r.pos.getY());
            c.putInt("Z", r.pos.getZ());
            c.putString("Name", r.name);
            c.putBoolean("Removed", r.removed);
            ListTag viewers = new ListTag();
            for (UUID id : r.viewers) {
                CompoundTag v = new CompoundTag();
                v.putUUID("Id", id);
                viewers.add(v);
            }
            c.put("Viewers", viewers);
            c.put("Lifetime", saveCounts(r.lifetime));
            c.put("DailyMc", saveBuckets(r.dailyMc));
            c.put("DailyReal", saveBuckets(r.dailyReal));
            list.add(c);
        }
        tag.put("Shops", list);
        return tag;
    }

    public static ShopSalesStats load(CompoundTag tag) {
        ShopSalesStats data = new ShopSalesStats();
        ListTag list = tag.getList("Shops", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ShopRecord r = new ShopRecord();
            r.dim = c.getString("Dim");
            r.pos = new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z"));
            r.name = c.getString("Name");
            r.removed = c.getBoolean("Removed");
            ListTag viewers = c.getList("Viewers", Tag.TAG_COMPOUND);
            for (int j = 0; j < viewers.size(); j++) r.viewers.add(viewers.getCompound(j).getUUID("Id"));
            loadCounts(c.getCompound("Lifetime"), r.lifetime);
            loadBuckets(c.getList("DailyMc", Tag.TAG_COMPOUND), r.dailyMc);
            loadBuckets(c.getList("DailyReal", Tag.TAG_COMPOUND), r.dailyReal);
            data.shops.put(c.getString("Key"), r);
        }
        return data;
    }

    private static CompoundTag saveCounts(Map<String, Counts> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Counts> e : map.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("S", e.getValue().sold);
            c.putLong("R", e.getValue().received);
            tag.put(e.getKey(), c);
        }
        return tag;
    }

    private static void loadCounts(CompoundTag tag, Map<String, Counts> into) {
        for (String id : tag.getAllKeys()) {
            CompoundTag c = tag.getCompound(id);
            Counts counts = new Counts();
            counts.sold = c.getLong("S");
            counts.received = c.getLong("R");
            into.put(id, counts);
        }
    }

    private static ListTag saveBuckets(Map<Long, Map<String, Counts>> buckets) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Map<String, Counts>> e : buckets.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Day", e.getKey());
            c.put("Counts", saveCounts(e.getValue()));
            list.add(c);
        }
        return list;
    }

    private static void loadBuckets(ListTag list, Map<Long, Map<String, Counts>> into) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Map<String, Counts> counts = new HashMap<>();
            loadCounts(c.getCompound("Counts"), counts);
            into.put(c.getLong("Day"), counts);
        }
    }

    /** Purge de sécurité (appelée à la consultation du Registre). */
    public void pruneAll(long mcToday, long realToday) {
        Iterator<ShopRecord> it = shops.values().iterator();
        while (it.hasNext()) {
            ShopRecord r = it.next();
            prune(r.dailyMc, mcToday);
            prune(r.dailyReal, realToday);
        }
        setDirty();
    }
}
