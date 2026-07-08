package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

/**
 * C2S : un joueur clique le bouton "coordonnées" (module communication). Le serveur
 * localise le villageois lié au comptoir et renvoie ses coordonnées au joueur.
 */
public record LocateVillagerPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<LocateVillagerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "locate_villager"));

    public static final StreamCodec<FriendlyByteBuf, LocateVillagerPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public LocateVillagerPacket decode(FriendlyByteBuf buf) { return new LocateVillagerPacket(buf.readBlockPos()); }
        @Override public void encode(FriendlyByteBuf buf, LocateVillagerPacket msg) { buf.writeBlockPos(msg.pos); }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LocateVillagerPacket msg, ServerPlayer player) {
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
    }
}
