package com.villagershop.network;

import com.mojang.authlib.GameProfile;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S : le propriétaire transfère la boutique à un co-propriétaire existant
 * (Maj+clic sur ★ dans la liste). L'ancien proprio devient co-proprio.
 */
public class TransferOwnerPacket {
    private final BlockPos pos;
    private final String name;

    public TransferOwnerPacket(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name;
    }

    public static void encode(TransferOwnerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.name);
    }

    public static TransferOwnerPacket decode(FriendlyByteBuf buf) {
        return new TransferOwnerPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(TransferOwnerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be)) return;
            if (!be.isOwner(player)) {
                player.displayClientMessage(Component.translatable("message.villagershop.not_owner"), true);
                return;
            }
            if (!be.hasAccess()) {
                player.displayClientMessage(Component.translatable("message.villagershop.no_access_module"), true);
                return;
            }
            MinecraftServer server = player.server;
            ServerPlayer online = server.getPlayerList().getPlayerByName(msg.name);
            java.util.UUID uuid = null;
            if (online != null) {
                uuid = online.getUUID();
            } else {
                Optional<GameProfile> profile = server.getProfileCache() == null
                        ? Optional.empty() : server.getProfileCache().get(msg.name);
                if (profile.isPresent()) uuid = profile.get().getId();
            }
            if (uuid == null || !be.transferOwner(uuid)) {
                player.displayClientMessage(Component.translatable("message.villagershop.transfer_fail"), true);
                return;
            }
            player.displayClientMessage(Component.translatable("message.villagershop.transfer_ok", msg.name), false);
            if (online != null) {
                online.displayClientMessage(Component.translatable("message.villagershop.transfer_received",
                        player.getGameProfile().getName(), be.getShopNameOrDefault()), false);
            }
            ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
