package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S : un joueur clique le bouton "coordonnées" (module communication). Le serveur
 * localise le villageois lié au comptoir et renvoie ses coordonnées au joueur.
 */
public class LocateVillagerPacket {
    private final BlockPos pos;

    public LocateVillagerPacket(BlockPos pos) { this.pos = pos; }

    public static void encode(LocateVillagerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static LocateVillagerPacket decode(FriendlyByteBuf buf) {
        return new LocateVillagerPacket(buf.readBlockPos());
    }

    public static void handle(LocateVillagerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be)) return;
            if (!be.canAccess(player)) return;
            if (!be.hasNotifier()) return; // module communication requis
            Villager v = be.findShopkeeper();
            if (v == null) {
                player.displayClientMessage(Component.translatable("message.villagershop.villager_missing"), false);
                return;
            }
            BlockPos vp = v.blockPosition();
            player.displayClientMessage(Component.translatable("message.villagershop.villager_at",
                    be.getShopNameOrDefault(), vp.getX(), vp.getY(), vp.getZ()), false);
        });
        ctx.get().setPacketHandled(true);
    }
}
