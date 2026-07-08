package com.villagershop.network;

import com.mojang.authlib.GameProfile;
import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** C2S : le propriétaire ajoute (add=true) ou retire (add=false) un joueur autorisé. */
public record ManageAllowedPacket(BlockPos pos, String name, boolean add) implements CustomPacketPayload {
    public static final Type<ManageAllowedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "manage_allowed"));

    public static final StreamCodec<FriendlyByteBuf, ManageAllowedPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public ManageAllowedPacket decode(FriendlyByteBuf buf) {
            return new ManageAllowedPacket(buf.readBlockPos(), buf.readUtf(), buf.readBoolean());
        }
        @Override public void encode(FriendlyByteBuf buf, ManageAllowedPacket msg) {
            buf.writeBlockPos(msg.pos);
            buf.writeUtf(msg.name);
            buf.writeBoolean(msg.add);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ManageAllowedPacket msg, ServerPlayer player) {
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
        if (!msg.add) {
            if (uuid.equals(be.getOwner()) || uuid.equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.villagershop.cant_remove"), true);
                return;
            }
        }
        if (msg.add) be.addAllowed(uuid, resolvedName);
        else be.removeAllowed(uuid);

        ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
    }
}
