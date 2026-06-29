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

/** C2S : le propriétaire ajoute (add=true) ou retire (add=false) un joueur autorisé. */
public class ManageAllowedPacket {
    private final BlockPos pos;
    private final String name;
    private final boolean add;

    public ManageAllowedPacket(BlockPos pos, String name, boolean add) {
        this.pos = pos;
        this.name = name;
        this.add = add;
    }

    public static void encode(ManageAllowedPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.name);
        buf.writeBoolean(msg.add);
    }

    public static ManageAllowedPacket decode(FriendlyByteBuf buf) {
        return new ManageAllowedPacket(buf.readBlockPos(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(ManageAllowedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be)) return;
            if (!be.isOwner(player)) {
                player.displayClientMessage(Component.translatable("message.villagershop.not_owner"), true);
                return;
            }
            MinecraftServer server = player.server;
            ServerPlayer online = server.getPlayerList().getPlayerByName(msg.name);
            java.util.UUID uuid = null;
            String resolvedName = msg.name;
            if (online != null) {
                uuid = online.getUUID();
                resolvedName = online.getGameProfile().getName();
            } else {
                Optional<GameProfile> profile = server.getProfileCache() == null
                        ? Optional.empty() : server.getProfileCache().get(msg.name);
                if (profile.isPresent()) {
                    uuid = profile.get().getId();
                    resolvedName = profile.get().getName();
                }
            }
            if (uuid == null) {
                player.displayClientMessage(Component.translatable("message.villagershop.unknown_player", msg.name), true);
                return;
            }
            if (msg.add) be.addAllowed(uuid, resolvedName);
            else be.removeAllowed(uuid);

            ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
