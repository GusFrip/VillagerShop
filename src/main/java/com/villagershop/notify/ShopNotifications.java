package com.villagershop.notify;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File d'attente des notifications de boutique (paratonnerre). Les proprios connectés
 * reçoivent le message immédiatement ; pour les autres, on stocke la notif et on la
 * délivre à leur prochaine connexion — en re-vérifiant que le blocage est toujours
 * d'actualité pour les soucis de supply/stock (la mort, elle, est toujours délivrée).
 */
public class ShopNotifications extends SavedData {

    public enum Reason { DEATH, SUPPLY, STOCK }

    private static final String NAME = "villagershop_notifications";

    public static class Note {
        public final Reason reason;
        public final String dim;
        public final BlockPos pos;
        public final String shopName;
        public Note(Reason reason, String dim, BlockPos pos, String shopName) {
            this.reason = reason; this.dim = dim; this.pos = pos; this.shopName = shopName;
        }
    }

    private final Map<UUID, List<Note>> pending = new HashMap<>();

    public static ShopNotifications get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(ShopNotifications::load, ShopNotifications::new, NAME);
    }

    /** Envoie aux proprios connectés ; met en file pour les déconnectés. */
    public static void dispatch(ServerLevel level, ShopBlockEntity be, Reason reason) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        Component msg = message(reason, be.getShopNameOrDefault(), be.getBlockPos());
        ShopNotifications data = get(server);
        String dim = level.dimension().location().toString();
        for (UUID id : be.getOwnersAndCoowners()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.sendSystemMessage(msg);
            } else {
                data.add(id, new Note(reason, dim, be.getBlockPos(), be.getShopNameOrDefault()));
            }
        }
    }

    private static Component message(Reason r, String shopName, BlockPos p) {
        return switch (r) {
            case DEATH  -> Component.translatable("message.villagershop.merchant_died", shopName, p.getX(), p.getY(), p.getZ());
            case SUPPLY -> Component.translatable("message.villagershop.no_supply",     shopName, p.getX(), p.getY(), p.getZ());
            case STOCK  -> Component.translatable("message.villagershop.no_stock",      shopName, p.getX(), p.getY(), p.getZ());
        };
    }

    public void add(UUID player, Note note) {
        List<Note> l = pending.computeIfAbsent(player, k -> new ArrayList<>());
        for (Note n : l) {
            if (n.reason == note.reason && n.pos.equals(note.pos) && n.dim.equals(note.dim)) return;
        }
        l.add(note);
        setDirty();
    }

    /** À la connexion : délivre les notifs en attente (re-check pour supply/stock). */
    public void deliver(ServerPlayer player) {
        List<Note> l = pending.remove(player.getUUID());
        if (l == null || l.isEmpty()) return;
        MinecraftServer server = player.getServer();
        for (Note n : l) {
            if (n.reason == Reason.DEATH) {
                player.sendSystemMessage(message(n.reason, n.shopName, n.pos));
                continue;
            }
            ServerLevel lvl = levelOf(server, n.dim);
            if (lvl != null && lvl.isLoaded(n.pos)
                    && lvl.getBlockEntity(n.pos) instanceof ShopBlockEntity be
                    && be.hasNotifier()
                    && ((n.reason == Reason.SUPPLY && be.hasSupplyProblem())
                     || (n.reason == Reason.STOCK  && be.hasStockProblem()))) {
                player.sendSystemMessage(message(n.reason, be.getShopNameOrDefault(), n.pos));
            }
        }
        setDirty();
    }

    private static ServerLevel levelOf(MinecraftServer server, String dim) {
        if (server == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        if (rl == null) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, List<Note>> e : pending.entrySet()) {
            for (Note n : e.getValue()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("Player", e.getKey());
                c.putString("Reason", n.reason.name());
                c.putString("Dim", n.dim);
                c.putInt("X", n.pos.getX());
                c.putInt("Y", n.pos.getY());
                c.putInt("Z", n.pos.getZ());
                c.putString("Name", n.shopName);
                list.add(c);
            }
        }
        tag.put("Pending", list);
        return tag;
    }

    public static ShopNotifications load(CompoundTag tag) {
        ShopNotifications d = new ShopNotifications();
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            UUID player = c.getUUID("Player");
            Reason reason;
            try { reason = Reason.valueOf(c.getString("Reason")); } catch (IllegalArgumentException ex) { continue; }
            Note n = new Note(reason, c.getString("Dim"),
                    new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z")), c.getString("Name"));
            d.pending.computeIfAbsent(player, k -> new ArrayList<>()).add(n);
        }
        return d;
    }
}
